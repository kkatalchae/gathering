package com.gathering.chat.application;

/**
 * 저장이 확정된 메시지를 구독자에게 전달하는 지점 (docs/adr/0003 "브로커는 단계적으로")
 * 지금은 로컬 Simple Broker 로 보내는 구현 하나. 스케일아웃 시 Redis pub/sub 으로 팬아웃하는 구현으로 바꾸며
 * 서비스·리스너 코드는 그대로 둔다
 */
public interface ChatMessageBroadcaster {

	void broadcast(ChatMessageSentEvent event);
}
