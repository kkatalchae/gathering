package com.gathering.chat.application;

import com.gathering.chat.presentation.dto.ChatMessageResponse;

/**
 * 메시지가 저장됐음을 알리는 애플리케이션 이벤트
 * 실시간 브로드캐스터(STOMP)가 구독한다 — 저장이 커밋된 뒤에만 전달되도록 리스너는 AFTER_COMMIT 으로 받는다 (docs/adr/0003)
 * 응답 DTO 를 그대로 싣는 이유: 구독자(클라이언트)가 REST 조회로 받는 것과 같은 형태여야 한다
 */
public record ChatMessageSentEvent(String roomTsid, ChatMessageResponse message) {
}
