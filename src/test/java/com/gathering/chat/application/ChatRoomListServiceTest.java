package com.gathering.chat.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatMessageKey;
import com.gathering.chat.domain.model.ChatMessageTsid;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;
import com.gathering.chat.domain.model.ChatRoomType;
import com.gathering.chat.domain.repository.ChatMessageKeyOnly;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.chat.presentation.dto.ChatRoomListResponse;
import com.gathering.chat.presentation.dto.ChatRoomSummaryResponse;
import com.gathering.chat.presentation.dto.ChatSenderSummary;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.user.domain.model.UsersEntity;

/**
 * ChatRoomListService 테스트
 * 멤버십 파생(모임 ∪ 일정), 안 읽은 수의 커서(읽음 위치 vs 참여 시각)와 상한, 버킷 순회, 정렬이 핵심이다
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomListServiceTest {

	private static final String USER_TSID = "01HQUSER000001";
	private static final String GATHERING_TSID = "01HQGATH000001";
	private static final String SCHEDULE_TSID = "01HQSCHD000001";
	private static final String GATHERING_ROOM = "01HQROOM000001";
	private static final String SCHEDULE_ROOM = "01HQROOM000002";

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private ChatRoomReadPositionRepository readPositionRepository;

	@Mock
	private GatheringParticipantRepository gatheringParticipantRepository;

	@Mock
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@Mock
	private ChatSenderResolver senderResolver;

	@InjectMocks
	private ChatRoomListService chatRoomListService;

	@Test
	@DisplayName("참여한 모임의 방과 일정의 방을 합쳐 마지막 메시지가 최근인 순으로 돌려주고 발신자는 한 번에 조회한다")
	void getMyRoomsMergesAndSorts() {
		// given: 모임 방은 메시지 있음(내가 읽음 처리한 뒤 2건 더), 일정 방은 메시지 없음
		Instant now = Instant.now();
		Instant joinedAt = now.minus(1, ChronoUnit.DAYS);
		ChatRoomEntity gatheringRoom = gatheringRoom(now.minus(2, ChronoUnit.DAYS));
		ChatRoomEntity scheduleRoom = scheduleRoom(now.minus(3, ChronoUnit.DAYS));
		givenMemberships(joinedAt, gatheringRoom, joinedAt, scheduleRoom);

		ChatMessageEntity read = ChatMessageEntity.text(GATHERING_ROOM, USER_TSID, "읽은 메시지");
		ChatMessageEntity unread1 = ChatMessageEntity.text(GATHERING_ROOM, "01HQUSER000002", "안 읽은 1");
		ChatMessageEntity unread2 = ChatMessageEntity.text(GATHERING_ROOM, "01HQUSER000002", "마지막");
		given(readPositionRepository.findByKeyUserTsid(USER_TSID)).willReturn(List.of(
			ChatRoomReadPositionEntity.of(USER_TSID, GATHERING_ROOM, read.getMessageTsid())));
		String currentBucket = ChatMessageBucket.of(now).getValue();
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(eq(GATHERING_ROOM), eq(currentBucket), any()))
			.willReturn(List.of(unread2));
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(eq(SCHEDULE_ROOM), eq(currentBucket), any()))
			.willReturn(List.of());
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), eq(currentBucket), eq(read.getMessageTsid()), any()))
			.willReturn(keys(unread1, unread2));
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(SCHEDULE_ROOM), eq(currentBucket), eq(ChatMessageTsid.floorOf(joinedAt)), any()))
			.willReturn(List.of());
		given(senderResolver.resolve(List.of("01HQUSER000002")))
			.willReturn(Map.of("01HQUSER000002", sender("01HQUSER000002", "상대")));

		// when
		ChatRoomListResponse response = chatRoomListService.getMyRooms(USER_TSID);

		// then
		assertThat(response.getRooms()).extracting(ChatRoomSummaryResponse::getRoomTsid)
			.containsExactly(GATHERING_ROOM, SCHEDULE_ROOM);
		ChatRoomSummaryResponse gathering = response.getRooms().getFirst();
		assertThat(gathering.getRoomType()).isEqualTo(ChatRoomType.GATHERING);
		assertThat(gathering.getTitle()).isEqualTo("한강 러닝크루");
		assertThat(gathering.getLastMessage().getContent()).isEqualTo("마지막");
		assertThat(gathering.getLastMessage().getSender().getNickname()).isEqualTo("상대");
		assertThat(gathering.getLastReadMessageTsid()).isEqualTo(read.getMessageTsid());
		assertThat(gathering.getUnreadCount()).isEqualTo(2);
		assertThat(gathering.isUnreadCountCapped()).isFalse();
		ChatRoomSummaryResponse schedule = response.getRooms().get(1);
		assertThat(schedule.getTitle()).isEqualTo("9월 정기 모임");
		assertThat(schedule.getLastMessage()).isNull();
		assertThat(schedule.getLastReadMessageTsid()).isNull();
		assertThat(schedule.getUnreadCount()).isZero();
		then(senderResolver).should(times(1)).resolve(any());
	}

	@Test
	@DisplayName("읽음 위치가 없으면 주체에 참여한 시각 이후를 안 읽은 것으로 센다")
	void unreadStartsAtJoinedAtWithoutReadPosition() {
		// given
		Instant now = Instant.now();
		Instant joinedAt = now.minus(2, ChronoUnit.HOURS);
		ChatRoomEntity room = gatheringRoom(now.minus(5, ChronoUnit.DAYS));
		givenMemberships(joinedAt, room, null, null);
		given(readPositionRepository.findByKeyUserTsid(USER_TSID)).willReturn(List.of());
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(any(), any(), any())).willReturn(List.of());
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			any(), any(), any(), any())).willReturn(List.of());

		// when
		chatRoomListService.getMyRooms(USER_TSID);

		// then: 커서는 참여 시각의 하한 TSID
		then(chatMessageRepository).should().findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), eq(ChatMessageBucket.of(now).getValue()), eq(ChatMessageTsid.floorOf(joinedAt)), any());
	}

	@Test
	@DisplayName("읽음 위치가 참여 시각보다 오래됐으면(재참여) 참여 시각이 커서가 된다")
	void unreadCursorIsLaterOfReadPositionAndJoinedAt() {
		// given: 읽음 위치는 어제, 재참여는 오늘
		Instant now = Instant.now();
		Instant joinedAt = now.minus(1, ChronoUnit.HOURS);
		String staleRead = ChatMessageTsid.floorOf(now.minus(1, ChronoUnit.DAYS));
		ChatRoomEntity room = gatheringRoom(now.minus(5, ChronoUnit.DAYS));
		givenMemberships(joinedAt, room, null, null);
		given(readPositionRepository.findByKeyUserTsid(USER_TSID))
			.willReturn(List.of(ChatRoomReadPositionEntity.of(USER_TSID, GATHERING_ROOM, staleRead)));
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(any(), any(), any())).willReturn(List.of());
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			any(), any(), any(), any())).willReturn(List.of());

		// when
		ChatRoomListResponse response = chatRoomListService.getMyRooms(USER_TSID);

		// then: 응답의 읽음 위치는 저장된 값 그대로, 카운트 커서는 참여 시각
		assertThat(response.getRooms().getFirst().getLastReadMessageTsid()).isEqualTo(staleRead);
		then(chatMessageRepository).should().findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), any(), eq(ChatMessageTsid.floorOf(joinedAt)), any());
	}

	@Test
	@DisplayName("안 읽은 수는 상한(100건)까지만 읽고 99+ 로 표시한다")
	void unreadCountIsCapped() {
		// given
		Instant now = Instant.now();
		ChatRoomEntity room = gatheringRoom(now.minus(1, ChronoUnit.DAYS));
		givenMemberships(now.minus(1, ChronoUnit.DAYS), room, null, null);
		given(readPositionRepository.findByKeyUserTsid(USER_TSID)).willReturn(List.of());
		ChatMessageEntity last = ChatMessageEntity.text(GATHERING_ROOM, USER_TSID, "마지막");
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(any(), any(), any())).willReturn(List.of(last));
		List<ChatMessageKeyOnly> hundredKeys = Collections.nCopies(ChatRoomListService.UNREAD_FETCH_LIMIT, keys(last).getFirst());
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			any(), any(), any(), any())).willReturn(hundredKeys);
		given(senderResolver.resolve(any())).willReturn(Map.of(USER_TSID, sender(USER_TSID, "나")));

		// when
		ChatRoomSummaryResponse summary = chatRoomListService.getMyRooms(USER_TSID).getRooms().getFirst();

		// then: 상한에 닿았으므로 이전 달 버킷은 읽지 않는다
		assertThat(summary.getUnreadCount()).isEqualTo(ChatRoomSummaryResponse.UNREAD_DISPLAY_MAX);
		assertThat(summary.isUnreadCountCapped()).isTrue();
		ArgumentCaptor<Limit> limit = ArgumentCaptor.forClass(Limit.class);
		then(chatMessageRepository).should(times(1)).findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			any(), any(), any(), limit.capture());
		assertThat(limit.getValue().max()).isEqualTo(ChatRoomListService.UNREAD_FETCH_LIMIT);
	}

	@Test
	@DisplayName("마지막 메시지와 안 읽은 수는 현재 달이 비어 있으면 이전 달 버킷으로 내려가되 커서·생성 월 아래로는 가지 않는다")
	void walksBucketsDownToBounds() {
		// given: 방은 석 달 전 생성, 참여는 두 달 전, 메시지는 지난 달에만 1건
		Instant now = Instant.now();
		ChatMessageBucket current = ChatMessageBucket.of(now);
		Instant joinedAt = middleOf(current.previous().previous());
		ChatRoomEntity room = gatheringRoom(middleOf(current.previous().previous().previous()));
		givenMemberships(joinedAt, room, null, null);
		given(readPositionRepository.findByKeyUserTsid(USER_TSID)).willReturn(List.of());
		ChatMessageEntity lastMonth = ChatMessageEntity.text(GATHERING_ROOM, USER_TSID, "지난 달");
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(eq(GATHERING_ROOM), eq(current.getValue()), any()))
			.willReturn(List.of());
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(
			eq(GATHERING_ROOM), eq(current.previous().getValue()), any())).willReturn(List.of(lastMonth));
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), eq(current.getValue()), any(), any())).willReturn(List.of());
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), eq(current.previous().getValue()), any(), any())).willReturn(keys(lastMonth));
		given(chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), eq(current.previous().previous().getValue()), any(), any())).willReturn(List.of());
		given(senderResolver.resolve(any())).willReturn(Map.of(USER_TSID, sender(USER_TSID, "나")));

		// when
		ChatRoomSummaryResponse summary = chatRoomListService.getMyRooms(USER_TSID).getRooms().getFirst();

		// then
		assertThat(summary.getLastMessage().getContent()).isEqualTo("지난 달");
		assertThat(summary.getUnreadCount()).isEqualTo(1);
		// 마지막 메시지: 현재 달(빈) → 지난 달(발견) 에서 멈춘다
		then(chatMessageRepository).should(times(2)).findByKeyRoomTsidAndKeyBucket(eq(GATHERING_ROOM), any(), any());
		// 안 읽은 수: 현재 달 → 지난 달 → 참여 월(두 달 전) 까지 세 버킷, 생성 월(석 달 전)은 참여 전이라 읽지 않는다
		then(chatMessageRepository).should(times(3)).findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			eq(GATHERING_ROOM), any(), any(), any());
	}

	@Test
	@DisplayName("참여한 모임/일정이 없으면 방 조회도 Cassandra 조회도 하지 않는다")
	void noMemberships() {
		// given
		given(gatheringParticipantRepository.findAllByUserTsid(USER_TSID)).willReturn(List.of());
		given(scheduleParticipantRepository.findAllByUserTsid(USER_TSID)).willReturn(List.of());
		given(readPositionRepository.findByKeyUserTsid(USER_TSID)).willReturn(List.of());

		// when
		ChatRoomListResponse response = chatRoomListService.getMyRooms(USER_TSID);

		// then
		assertThat(response.getRooms()).isEmpty();
		then(chatRoomRepository).shouldHaveNoInteractions();
		then(chatMessageRepository).shouldHaveNoInteractions();
	}

	/**
	 * 모임 참여(gatheringJoinedAt/gatheringRoom) 와 일정 참여(scheduleJoinedAt/scheduleRoom) 를 스텁한다. null 이면 참여 없음
	 */
	private void givenMemberships(Instant gatheringJoinedAt, ChatRoomEntity gatheringRoom,
		Instant scheduleJoinedAt, ChatRoomEntity scheduleRoom) {
		if (gatheringRoom != null) {
			given(gatheringParticipantRepository.findAllByUserTsid(USER_TSID)).willReturn(List.of(
				GatheringParticipantEntity.builder().gatheringTsid(GATHERING_TSID).userTsid(USER_TSID)
					.role(ParticipantRole.MEMBER).joinedAt(gatheringJoinedAt).build()));
			given(chatRoomRepository.findAllByGatheringTsidInWithGathering(List.of(GATHERING_TSID)))
				.willReturn(List.of(gatheringRoom));
		} else {
			given(gatheringParticipantRepository.findAllByUserTsid(USER_TSID)).willReturn(List.of());
		}
		if (scheduleRoom != null) {
			given(scheduleParticipantRepository.findAllByUserTsid(USER_TSID)).willReturn(List.of(
				ScheduleParticipantEntity.builder().scheduleTsid(SCHEDULE_TSID).userTsid(USER_TSID)
					.joinedAt(scheduleJoinedAt).build()));
			given(chatRoomRepository.findAllByScheduleTsidInWithSchedule(List.of(SCHEDULE_TSID)))
				.willReturn(List.of(scheduleRoom));
		} else {
			given(scheduleParticipantRepository.findAllByUserTsid(USER_TSID)).willReturn(List.of());
		}
	}

	/** 날짜 뺄셈으로 월을 넘나들면 테스트가 실행일에 따라 흔들리므로 버킷의 한가운데 시각을 쓴다 */
	private Instant middleOf(ChatMessageBucket bucket) {
		return Instant.parse(bucket.getValue() + "-15T00:00:00Z");
	}

	private ChatRoomEntity gatheringRoom(Instant createdAt) {
		ChatRoomEntity room = mock(ChatRoomEntity.class);
		lenient().when(room.getTsid()).thenReturn(GATHERING_ROOM);
		lenient().when(room.getRoomType()).thenReturn(ChatRoomType.GATHERING);
		lenient().when(room.getGatheringTsid()).thenReturn(GATHERING_TSID);
		lenient().when(room.getCreatedAt()).thenReturn(createdAt);
		lenient().when(room.getGathering()).thenReturn(GatheringEntity.builder().name("한강 러닝크루").build());
		return room;
	}

	private ChatRoomEntity scheduleRoom(Instant createdAt) {
		ChatRoomEntity room = mock(ChatRoomEntity.class);
		lenient().when(room.getTsid()).thenReturn(SCHEDULE_ROOM);
		lenient().when(room.getRoomType()).thenReturn(ChatRoomType.SCHEDULE);
		lenient().when(room.getScheduleTsid()).thenReturn(SCHEDULE_TSID);
		lenient().when(room.getCreatedAt()).thenReturn(createdAt);
		lenient().when(room.getSchedule()).thenReturn(ScheduleEntity.builder().title("9월 정기 모임").build());
		return room;
	}

	private List<ChatMessageKeyOnly> keys(ChatMessageEntity... messages) {
		return IntStream.range(0, messages.length)
			.mapToObj(i -> (ChatMessageKeyOnly)messages[i]::getKey)
			.toList();
	}

	private ChatSenderSummary sender(String tsid, String nickname) {
		return ChatSenderSummary.from(
			UsersEntity.builder().tsid(tsid).email(tsid + "@example.com").name("이름").nickname(nickname).build());
	}
}
