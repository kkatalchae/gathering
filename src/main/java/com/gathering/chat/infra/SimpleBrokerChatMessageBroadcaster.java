package com.gathering.chat.infra;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.gathering.chat.application.ChatMessageBroadcaster;
import com.gathering.chat.application.ChatMessageSentEvent;

import lombok.RequiredArgsConstructor;

/**
 * 이 인스턴스의 Simple Broker 로 브로드캐스트 — 인스턴스 1개 구성용
 */
@Component
@RequiredArgsConstructor
public class SimpleBrokerChatMessageBroadcaster implements ChatMessageBroadcaster {

	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public void broadcast(ChatMessageSentEvent event) {
		messagingTemplate.convertAndSend(ChatDestinations.room(event.roomTsid()), event.message());
	}
}
