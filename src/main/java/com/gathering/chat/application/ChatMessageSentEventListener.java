package com.gathering.chat.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메시지 저장이 커밋된 뒤에만 브로드캐스트한다 (docs/adr/0003 "DB 가 source of truth, 소켓은 알림 채널")
 * 트랜잭션이 롤백되면 이벤트는 버려진다 — 저장되지 않은 메시지가 화면에 뜨는 일이 없다
 * 브로드캐스트 실패는 이미 저장된 메시지에 영향을 주지 않으므로 경고만 남긴다. 놓친 클라이언트는 재접속 시 after 조회로 보충한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSentEventListener {

	private final ChatMessageBroadcaster broadcaster;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onMessageSent(ChatMessageSentEvent event) {
		try {
			broadcaster.broadcast(event);
		} catch (RuntimeException e) {
			log.warn("채팅 메시지 브로드캐스트 실패 (저장은 완료됨): roomTsid={}, messageTsid={}, cause={}",
				event.roomTsid(), event.message().getMessageTsid(), e.getMessage());
		}
	}
}
