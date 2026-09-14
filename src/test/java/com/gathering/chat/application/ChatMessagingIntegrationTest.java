package com.gathering.chat.application;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.chat.presentation.dto.ChatMessageListResponse;
import com.gathering.chat.presentation.dto.ChatMessageResponse;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * 채팅 통합 테스트 — MySQL(멤버십, 방) 과 Cassandra(메시지, 읽음 위치) 를 실제로 오가며 전송 → 조회 → 보충 → 읽음 → 방 삭제를 검증한다
 */
@SpringBootTest
class ChatMessagingIntegrationTest {

	@Autowired
	private ChatMessageService chatMessageService;

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private GatheringRepository gatheringRepository;

	@Autowired
	private GatheringParticipantRepository participantRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomReadPositionRepository readPositionRepository;

	private UsersEntity member;
	private UsersEntity stranger;
	private RegionEntity region;
	private GatheringEntity gathering;
	private ChatRoomEntity room;

	@BeforeEach
	void setUp() {
		member = usersRepository.save(user("member"));
		stranger = usersRepository.save(user("stranger"));
		region = regionRepository.save(RegionEntity.builder()
			.name("채팅 테스트 지역").code("CH" + System.nanoTime() % 100000).depth(1).path("/ch/").build());
		gathering = gatheringRepository.save(GatheringEntity.builder()
			.name("채팅 테스트 모임").regionTsid(region.getTsid()).category(GatheringCategory.SPORTS).build());
		participantRepository.save(GatheringParticipantEntity.builder()
			.gatheringTsid(gathering.getTsid()).userTsid(member.getTsid()).role(ParticipantRole.OWNER).build());
		room = chatRoomService.createForGathering(gathering.getTsid());
	}

	@AfterEach
	void tearDown() {
		transactionTemplate.executeWithoutResult(status -> {
			chatRoomRepository.findByGatheringTsid(gathering.getTsid())
				.ifPresent(r -> chatRoomRepository.deleteAllByTsidIn(List.of(r.getTsid())));
			participantRepository.deleteAllByGatheringTsid(gathering.getTsid());
			gatheringRepository.delete(gathering);
			regionRepository.delete(region);
			usersRepository.deleteAll(List.of(member, stranger));
		});
		chatMessageRepository.deleteByKeyRoomTsidAndKeyBucket(room.getTsid(), ChatMessageBucket.of(Instant.now()).getValue());
		readPositionRepository.deleteByKeyUserTsid(member.getTsid());
	}

	@Test
	@DisplayName("멤버는 메시지를 보내고 최신순으로 조회하며, 마지막으로 받은 메시지 이후를 보충 조회할 수 있다")
	void sendListAndCatchUp() {
		// given
		ChatMessageResponse first = chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "첫 번째");
		ChatMessageResponse second = chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "두 번째");
		ChatMessageResponse third = chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "세 번째");

		// when: 최신순 2건
		ChatMessageListResponse latest = chatMessageService.getMessagesBefore(room.getTsid(), member.getTsid(), null, 2);

		// then
		assertThat(latest.getMessages()).extracting(ChatMessageResponse::getContent).containsExactly("세 번째", "두 번째");
		assertThat(latest.getHasNext()).isTrue();
		assertThat(latest.getNextCursor()).isEqualTo(second.getMessageTsid());
		assertThat(latest.getMessages().getFirst().getSender().getName()).isEqualTo("member");

		// when: 커서로 이전 페이지
		ChatMessageListResponse older = chatMessageService.getMessagesBefore(
			room.getTsid(), member.getTsid(), latest.getNextCursor(), 2);

		// then
		assertThat(older.getMessages()).extracting(ChatMessageResponse::getContent).containsExactly("첫 번째");
		assertThat(older.getHasNext()).isFalse();

		// when: 첫 번째까지 받은 클라이언트가 재접속해 보충
		ChatMessageListResponse catchUp = chatMessageService.getMessagesAfter(
			room.getTsid(), member.getTsid(), first.getMessageTsid(), 50);

		// then: 오래된 순
		assertThat(catchUp.getMessages()).extracting(ChatMessageResponse::getContent).containsExactly("두 번째", "세 번째");
		assertThat(third.getMessageTsid()).isGreaterThan(second.getMessageTsid());
	}

	@Test
	@DisplayName("모임 참여자가 아니면 메시지를 보내거나 읽을 수 없다")
	void strangerIsDenied() {
		assertThatThrownBy(() -> chatMessageService.sendMessage(room.getTsid(), stranger.getTsid(), "끼어들기"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		assertThatThrownBy(() -> chatMessageService.getMessagesBefore(room.getTsid(), stranger.getTsid(), null, 50))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ACCESS_DENIED);
	}

	@Test
	@DisplayName("읽음 위치는 앞으로만 옮겨지고 Cassandra 에 저장된다")
	void readPositionOnlyAdvances() {
		// given
		ChatMessageResponse first = chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "첫 번째");
		ChatMessageResponse second = chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "두 번째");

		// when
		chatMessageService.markAsRead(room.getTsid(), member.getTsid(), second.getMessageTsid());
		chatMessageService.markAsRead(room.getTsid(), member.getTsid(), first.getMessageTsid());

		// then
		assertThat(readPositionRepository.findByKeyUserTsidAndKeyRoomTsid(member.getTsid(), room.getTsid()))
			.get().extracting(p -> p.getLastReadMessageTsid()).isEqualTo(second.getMessageTsid());
	}

	@Test
	@DisplayName("모임 채팅방을 삭제하면 row 는 즉시, 메시지 파티션은 커밋 후 사라진다")
	void deleteRoomRemovesMessagesAfterCommit() {
		// given
		chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "곧 사라질 메시지");
		String bucket = ChatMessageBucket.of(Instant.now()).getValue();
		assertThat(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(room.getTsid(), bucket,
			Limit.of(10))).hasSize(1);

		// when
		transactionTemplate.executeWithoutResult(status -> chatRoomService.deleteForGathering(gathering.getTsid()));

		// then
		assertThat(chatRoomRepository.findByGatheringTsid(gathering.getTsid())).isEmpty();
		assertThat(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(room.getTsid(), bucket,
			Limit.of(10))).isEmpty();
	}

	private UsersEntity user(String prefix) {
		return UsersEntity.builder().email(prefix + "-" + System.nanoTime() + "@example.com").name(prefix).build();
	}
}
