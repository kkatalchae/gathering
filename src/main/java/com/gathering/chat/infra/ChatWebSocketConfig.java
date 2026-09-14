package com.gathering.chat.infra;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * 채팅 실시간 수신 채널 (docs/adr/0003)
 * - 엔드포인트 /ws — 핸드셰이크는 permitAll, 인증은 CONNECT 프레임에서 (ChatStompChannelInterceptor)
 * - 브로커: 지금은 인메모리 Simple Broker. 인스턴스가 늘면 ChatMessageBroadcaster 구현체를 Redis pub/sub 팬아웃으로 바꾼다
 * - 클라이언트가 SEND 할 애플리케이션 목적지는 두지 않는다 — 전송은 REST 다
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	public static final String ENDPOINT = "/ws";

	private final ChatStompChannelInterceptor chatStompChannelInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(ENDPOINT).setAllowedOriginPatterns("*");
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(chatStompChannelInterceptor);
	}
}
