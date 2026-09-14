package com.gathering.chat.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;
import com.gathering.chat.domain.policy.ChatRoomPolicy;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.chat.presentation.dto.ChatMessageListResponse;
import com.gathering.chat.presentation.dto.ChatMessageResponse;
import com.gathering.chat.presentation.dto.ChatReadPositionResponse;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * ChatMessageService 테스트
 * 버킷 경계를 넘는 페이징과 발신자 조립(IN 한 번)이 핵심이다
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

	private static final String ROOM_TSID = "01HQROOM000001";
	private static final String USER_TSID = "01HQUSER000001";

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private ChatRoomReadPositionRepository readPositionRepository;

	@Mock
	private UsersRepository usersRepository;

	@Mock
	private ChatRoomPolicy chatRoomPolicy;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ChatMessageService chatMessageService;

	@Test
	@DisplayName("메시지를 보내면 멤버십 검증 후 저장하고 발신자 정보를 실은 이벤트를 발행한다")
	void sendMessagePublishesEvent() {
		// given
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		given(chatMessageRepository.save(any(ChatMessageEntity.class))).willAnswer(inv -> inv.getArgument(0));
		given(usersRepository.findById(USER_TSID)).willReturn(Optional.of(user(USER_TSID, "러닝맨")));

		// when
		ChatMessageResponse response = chatMessageService.sendMessage(ROOM_TSID, USER_TSID, "안녕하세요");

		// then
		then(chatRoomPolicy).should().validateMember(room, USER_TSID);
		assertThat(response.getContent()).isEqualTo("안녕하세요");
		assertThat(response.getSender().getNickname()).isEqualTo("러닝맨");

		ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
		then(eventPublisher).should().publishEvent(captor.capture());
		assertThat(captor.getValue().roomTsid()).isEqualTo(ROOM_TSID);
		assertThat(captor.getValue().message().getMessageTsid()).isEqualTo(response.getMessageTsid());
	}

	@Test
	@DisplayName("멤버가 아니면 저장도 이벤트 발행도 하지 않는다")
	void sendMessageDeniedForNonMember() {
		// given
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		willThrow(new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED))
			.given(chatRoomPolicy).validateMember(room, USER_TSID);

		// when & then
		assertThatThrownBy(() -> chatMessageService.sendMessage(ROOM_TSID, USER_TSID, "안녕하세요"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		then(chatMessageRepository).shouldHaveNoInteractions();
		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("빈 내용은 저장 전에 거부된다")
	void sendMessageRejectsBlankContent() {
		// given
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));

		// when & then
		assertThatThrownBy(() -> chatMessageService.sendMessage(ROOM_TSID, USER_TSID, "   "))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
		then(chatMessageRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("첫 페이지가 현재 달에서 부족하면 이전 달 버킷으로 이어서 채우고 발신자는 한 번에 조회한다")
	void getMessagesBeforeCrossesBucketBoundary() {
		// given: 방은 두 달 전 생성, 현재 달에 1건, 지난 달에 2건
		Instant now = Instant.now();
		ChatRoomEntity room = roomCreatedAt(now.minus(62, ChronoUnit.DAYS));
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		String currentBucket = ChatMessageBucket.of(now).getValue();
		String previousBucket = ChatMessageBucket.of(now).previous().getValue();

		ChatMessageEntity newest = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "이번 달");
		ChatMessageEntity older = ChatMessageEntity.text(ROOM_TSID, "01HQUSER000002", "지난 달 2");
		ChatMessageEntity oldest = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "지난 달 1");
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(eq(ROOM_TSID), eq(currentBucket), any(Limit.class)))
			.willReturn(List.of(newest));
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(eq(ROOM_TSID), eq(previousBucket), any(Limit.class)))
			.willReturn(List.of(older, oldest));
		given(usersRepository.findAllById(List.of(USER_TSID, "01HQUSER000002")))
			.willReturn(List.of(user(USER_TSID, "나"), user("01HQUSER000002", "상대")));

		// when: size=2 → 3건 모이면 hasNext
		ChatMessageListResponse response = chatMessageService.getMessagesBefore(ROOM_TSID, USER_TSID, null, 2);

		// then
		assertThat(response.getMessages()).extracting(ChatMessageResponse::getContent)
			.containsExactly("이번 달", "지난 달 2");
		assertThat(response.getHasNext()).isTrue();
		assertThat(response.getNextCursor()).isEqualTo(older.getMessageTsid());
		assertThat(response.getMessages().get(1).getSender().getNickname()).isEqualTo("상대");
		then(usersRepository).should(times(1)).findAllById(any());
	}

	@Test
	@DisplayName("방 생성 월보다 오래된 버킷은 조회하지 않는다")
	void getMessagesBeforeStopsAtRoomCreationMonth() {
		// given: 이번 달에 만들어진 방, 메시지 없음
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucket(eq(ROOM_TSID), any(), any(Limit.class)))
			.willReturn(List.of());

		// when
		ChatMessageListResponse response = chatMessageService.getMessagesBefore(ROOM_TSID, USER_TSID, null, 50);

		// then: 현재 달 버킷 한 번만 조회한다
		assertThat(response.getMessages()).isEmpty();
		assertThat(response.getHasNext()).isFalse();
		then(chatMessageRepository).should(times(1)).findByKeyRoomTsidAndKeyBucket(eq(ROOM_TSID), any(), any());
		then(usersRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("before 커서가 주어지면 그 버킷에서 커서보다 오래된 것부터 읽는다")
	void getMessagesBeforeWithCursor() {
		// given
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		ChatMessageEntity cursorMessage = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "커서");
		ChatMessageEntity olderMessage = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "더 오래됨");
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidLessThan(
			eq(ROOM_TSID), eq(ChatMessageBucket.of(Instant.now()).getValue()), eq(cursorMessage.getMessageTsid()),
			any(Limit.class))).willReturn(List.of(olderMessage));
		given(usersRepository.findAllById(any())).willReturn(List.of(user(USER_TSID, "나")));

		// when
		ChatMessageListResponse response =
			chatMessageService.getMessagesBefore(ROOM_TSID, USER_TSID, cursorMessage.getMessageTsid(), 50);

		// then
		assertThat(response.getMessages()).extracting(ChatMessageResponse::getContent).containsExactly("더 오래됨");
		assertThat(response.getHasNext()).isFalse();
	}

	@Test
	@DisplayName("after 조회는 커서 버킷부터 현재 버킷까지 올라가며 오래된 순으로 채운다")
	void getMessagesAfterClimbsToCurrentBucket() {
		// given: 커서는 지난 달, 지난 달에 1건·이번 달에 1건이 그 이후
		Instant now = Instant.now();
		ChatRoomEntity room = roomCreatedAt(now.minus(62, ChronoUnit.DAYS));
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		String currentBucket = ChatMessageBucket.of(now).getValue();
		String previousBucket = ChatMessageBucket.of(now).previous().getValue();
		// 지난 달 시각의 TSID 를 커서로 흉내 낸다: 버킷 계산만 필요하므로 TSID 생성 시각을 지난 달로 맞춘다
		String cursor = com.github.f4b6a3.tsid.TsidCreator.getTsid().toString();
		ChatMessageEntity lastMonth = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "지난 달 이후");
		ChatMessageEntity thisMonth = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "이번 달");
		given(chatMessageRepository.findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThanOrderByKeyMessageTsidAsc(
			eq(ROOM_TSID), eq(currentBucket), eq(cursor), any(Limit.class))).willReturn(List.of(lastMonth, thisMonth));
		given(usersRepository.findAllById(any())).willReturn(List.of(user(USER_TSID, "나")));

		// when: 커서가 이번 달 TSID 이므로 이번 달 버킷만 읽는다
		ChatMessageListResponse response = chatMessageService.getMessagesAfter(ROOM_TSID, USER_TSID, cursor, 50);

		// then
		assertThat(response.getMessages()).extracting(ChatMessageResponse::getContent)
			.containsExactly("지난 달 이후", "이번 달");
		assertThat(response.getHasNext()).isFalse();
		then(chatMessageRepository).should(never())
			.findByKeyRoomTsidAndKeyBucketOrderByKeyMessageTsidAsc(eq(ROOM_TSID), eq(previousBucket), any());
	}

	@Test
	@DisplayName("형식이 잘못된 커서는 INVALID_CURSOR 로 거부된다")
	void invalidCursor() {
		// given
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));

		// when & then
		assertThatThrownBy(() -> chatMessageService.getMessagesBefore(ROOM_TSID, USER_TSID, "not-a-tsid", 50))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURSOR);
	}

	@Test
	@DisplayName("읽음 위치가 없으면 새로 만들고, 있으면 앞으로 옮겨졌을 때만 저장한다")
	void markAsRead() {
		// given
		ChatRoomEntity room = roomCreatedAt(Instant.now());
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		given(readPositionRepository.findByKeyUserTsidAndKeyRoomTsid(USER_TSID, ROOM_TSID))
			.willReturn(Optional.empty())
			.willReturn(Optional.of(ChatRoomReadPositionEntity.of(USER_TSID, ROOM_TSID, "0HZZZZZZZZZZ5")));
		given(readPositionRepository.save(any(ChatRoomReadPositionEntity.class))).willAnswer(inv -> inv.getArgument(0));

		// when: 처음 → 생성
		ChatReadPositionResponse created = chatMessageService.markAsRead(ROOM_TSID, USER_TSID, "0HZZZZZZZZZZ5");
		// when: 뒤로 가는 요청 → 저장하지 않음
		ChatReadPositionResponse unchanged = chatMessageService.markAsRead(ROOM_TSID, USER_TSID, "0HZZZZZZZZZZ1");

		// then
		assertThat(created.getLastReadMessageTsid()).isEqualTo("0HZZZZZZZZZZ5");
		assertThat(unchanged.getLastReadMessageTsid()).isEqualTo("0HZZZZZZZZZZ5");
		then(readPositionRepository).should(times(1)).save(any());
	}

	private ChatRoomEntity roomCreatedAt(Instant createdAt) {
		// 다른 given(...) 안에서 호출되지 않도록 각 테스트에서 먼저 만든다. 모든 테스트가 두 getter 를 다 쓰지는 않으므로 lenient
		ChatRoomEntity room = mock(ChatRoomEntity.class);
		lenient().when(room.getTsid()).thenReturn(ROOM_TSID);
		lenient().when(room.getCreatedAt()).thenReturn(createdAt);
		return room;
	}

	private UsersEntity user(String tsid, String nickname) {
		return UsersEntity.builder().tsid(tsid).email(tsid + "@example.com").name("이름").nickname(nickname).build();
	}
}
