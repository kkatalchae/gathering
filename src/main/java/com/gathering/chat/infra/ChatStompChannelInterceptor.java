package com.gathering.chat.infra;

import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.gathering.auth.infra.AuthConstants;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.policy.ChatRoomPolicy;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 인바운드 프레임 인증·인가 (docs/adr/0003)
 * - CONNECT: Authorization 네이티브 헤더의 Bearer 토큰을 검증하고 세션에 사용자를 붙인다
 * - SUBSCRIBE: /topic/rooms/{roomTsid} 만 허용하며, 그 방의 멤버(참여자)인지 확인한다
 * 거부는 MessageDeliveryException 으로 던지며, Spring 이 ERROR 프레임(message 헤더 = ErrorCode 이름)으로 바꿔 보낸다
 * 전송(SEND)은 받지 않는다 — 전송은 REST 다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStompChannelInterceptor implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomPolicy chatRoomPolicy;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}

		// 명시 분기: 인증이 필요한 프레임만 다루고 나머지(DISCONNECT, UNSUBSCRIBE, 하트비트 등)는 그대로 통과
		switch (accessor.getCommand()) {
			case CONNECT, STOMP -> authenticate(message, accessor);
			case SUBSCRIBE -> authorizeSubscription(message, accessor);
			case SEND -> throw reject(message, ErrorCode.CHAT_ROOM_ACCESS_DENIED);
			default -> {
			}
		}
		return message;
	}

	private void authenticate(Message<?> message, StompHeaderAccessor accessor) {
		String bearer = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
		if (bearer == null || !bearer.startsWith(AuthConstants.BEARER_PREFIX)) {
			throw reject(message, ErrorCode.ACCESS_TOKEN_MISSING);
		}
		String token = bearer.substring(AuthConstants.BEARER_PREFIX.length());

		try {
			jwtTokenProvider.validateAccessToken(token);
		} catch (BusinessException e) {
			throw reject(message, e.getErrorCode());
		}

		accessor.setUser(new StompUserPrincipal(jwtTokenProvider.getTsidFromToken(token)));
	}

	private void authorizeSubscription(Message<?> message, StompHeaderAccessor accessor) {
		Principal user = accessor.getUser();
		if (!(user instanceof StompUserPrincipal principal)) {
			throw reject(message, ErrorCode.ACCESS_TOKEN_MISSING);
		}

		String roomTsid = ChatDestinations.roomTsidOf(accessor.getDestination())
			.orElseThrow(() -> reject(message, ErrorCode.CHAT_ROOM_NOT_FOUND));
		ChatRoomEntity room = chatRoomRepository.findById(roomTsid)
			.orElseThrow(() -> reject(message, ErrorCode.CHAT_ROOM_NOT_FOUND));

		if (!chatRoomPolicy.isMember(room, principal.userTsid())) {
			throw reject(message, ErrorCode.CHAT_ROOM_ACCESS_DENIED);
		}
	}

	private MessageDeliveryException reject(Message<?> message, ErrorCode errorCode) {
		log.debug("STOMP 프레임 거부: {}", errorCode.name());
		// description 이 ERROR 프레임의 message 헤더가 된다 — 클라이언트는 이 값으로 ErrorCode 를 식별한다
		return new MessageDeliveryException(message, errorCode.name());
	}
}
