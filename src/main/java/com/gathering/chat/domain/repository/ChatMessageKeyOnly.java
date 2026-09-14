package com.gathering.chat.domain.repository;

import com.gathering.chat.domain.model.ChatMessageKey;

/**
 * 메시지 키만 읽는 프로젝션 — 개수만 필요할 때 content 를 전송하지 않기 위한 것
 * CQL 의 COUNT 는 LIMIT 으로 잘리지 않으므로(집계 뒤에 적용) 상한 있는 개수는 키를 LIMIT 만큼 읽어 센다 (docs/adr/0002)
 */
public interface ChatMessageKeyOnly {

	ChatMessageKey getKey();
}
