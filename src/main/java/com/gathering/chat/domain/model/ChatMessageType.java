package com.gathering.chat.domain.model;

public enum ChatMessageType {
	/** 사용자가 보낸 일반 메시지 */
	TEXT,
	/** 입장/퇴장 등 서버가 생성한 안내 메시지 (보낸 사람 없음) */
	SYSTEM
}
