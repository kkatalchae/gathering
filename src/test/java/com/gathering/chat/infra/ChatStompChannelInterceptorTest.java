package com.gathering.chat.infra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.policy.ChatRoomPolicy;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ChatStompChannelInterceptor 테스트 — CONNECT 인증과 SUBSCRIBE 인가
 */
@ExtendWith(MockitoExtension.class)
class ChatStompChannelInterceptorTest {

	private static final String USER_TSID = "01HQUSER000001";
	private static final String ROOM_TSID = "01HQROOM000001";

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatRoomPolicy chatRoomPolicy;

	@Mock
	private MessageChannel channel;

	@InjectMocks
	private ChatStompChannelInterceptor interceptor;

	@Test
	@DisplayName("유효한 Bearer 토큰으로 CONNECT 하면 세션에 사용자가 붙는다")
	void connectWithValidToken() {
		// given
		given(jwtTokenProvider.getTsidFromToken("valid")).willReturn(USER_TSID);
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.addNativeHeader("Authorization", "Bearer valid");

		// when
		interceptor.preSend(message(accessor), channel);

		// then
		assertThat(accessor.getUser()).isInstanceOf(StompUserPrincipal.class);
		assertThat(accessor.getUser().getName()).isEqualTo(USER_TSID);
	}

	@Test
	@DisplayName("토큰이 없으면 CONNECT 가 ACCESS_TOKEN_MISSING 으로 거부된다")
	void connectWithoutToken() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
			.isInstanceOf(MessageDeliveryException.class)
			.hasMessage(ErrorCode.ACCESS_TOKEN_MISSING.name());
	}

	@Test
	@DisplayName("만료된 토큰이면 CONNECT 가 토큰 검증 에러 코드로 거부된다")
	void connectWithExpiredToken() {
		willThrow(new BusinessException(ErrorCode.ACCESS_TOKEN_EXPIRED)).given(jwtTokenProvider).validateAccessToken("expired");
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.addNativeHeader("Authorization", "Bearer expired");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
			.isInstanceOf(MessageDeliveryException.class)
			.hasMessage(ErrorCode.ACCESS_TOKEN_EXPIRED.name());
	}

	@Test
	@DisplayName("채팅방 멤버는 그 방의 토픽을 구독할 수 있다")
	void subscribeAsMember() {
		// given
		ChatRoomEntity room = ChatRoomEntity.forGathering("01HQGATHERING1");
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		given(chatRoomPolicy.isMember(room, USER_TSID)).willReturn(true);
		StompHeaderAccessor accessor = subscribe(ChatDestinations.room(ROOM_TSID), new StompUserPrincipal(USER_TSID));

		// when & then
		assertThatCode(() -> interceptor.preSend(message(accessor), channel)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("멤버가 아니면 구독이 CHAT_ROOM_ACCESS_DENIED 로 거부된다")
	void subscribeAsNonMember() {
		ChatRoomEntity room = ChatRoomEntity.forGathering("01HQGATHERING1");
		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.of(room));
		given(chatRoomPolicy.isMember(room, USER_TSID)).willReturn(false);
		StompHeaderAccessor accessor = subscribe(ChatDestinations.room(ROOM_TSID), new StompUserPrincipal(USER_TSID));

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
			.isInstanceOf(MessageDeliveryException.class)
			.hasMessage(ErrorCode.CHAT_ROOM_ACCESS_DENIED.name());
	}

	@Test
	@DisplayName("인증 없이 구독하면 거부된다")
	void subscribeWithoutPrincipal() {
		StompHeaderAccessor accessor = subscribe(ChatDestinations.room(ROOM_TSID), null);

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
			.isInstanceOf(MessageDeliveryException.class)
			.hasMessage(ErrorCode.ACCESS_TOKEN_MISSING.name());
		then(chatRoomRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("채팅방 토픽이 아닌 목적지나 없는 방은 구독할 수 없다")
	void subscribeToUnknownDestination() {
		StompHeaderAccessor other = subscribe("/topic/anything", new StompUserPrincipal(USER_TSID));
		assertThatThrownBy(() -> interceptor.preSend(message(other), channel))
			.isInstanceOf(MessageDeliveryException.class)
			.hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.name());

		given(chatRoomRepository.findById(ROOM_TSID)).willReturn(Optional.empty());
		StompHeaderAccessor missing = subscribe(ChatDestinations.room(ROOM_TSID), new StompUserPrincipal(USER_TSID));
		assertThatThrownBy(() -> interceptor.preSend(message(missing), channel))
			.isInstanceOf(MessageDeliveryException.class)
			.hasMessage(ErrorCode.CHAT_ROOM_NOT_FOUND.name());
	}

	@Test
	@DisplayName("SEND 프레임은 받지 않는다 — 전송은 REST 다")
	void sendIsRejected() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/app/anything");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
			.isInstanceOf(MessageDeliveryException.class);
	}

	@Test
	@DisplayName("DISCONNECT 같은 나머지 프레임은 손대지 않고 통과시킨다")
	void otherFramesPassThrough() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<byte[]> message = message(accessor);

		assertThat(interceptor.preSend(message, channel)).isSameAs(message);
	}

	private StompHeaderAccessor subscribe(String destination, StompUserPrincipal user) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination(destination);
		accessor.setSubscriptionId("sub-0");
		if (user != null) {
			accessor.setUser(user);
		}
		return accessor;
	}

	/** 실제 STOMP 핸들러처럼 헤더를 mutable 로 남겨 인터셉터가 사용자를 붙일 수 있게 한다 */
	private Message<byte[]> message(StompHeaderAccessor accessor) {
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
