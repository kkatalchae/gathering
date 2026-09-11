package com.gathering.chat.domain.model;

/**
 * 채팅방이 어느 주체에 붙어 있는지
 * 주체별로 방이 하나씩 자동 생성되며, 멤버십은 그 주체의 참여자 테이블에서 파생된다
 */
public enum ChatRoomType {
	/** 모임 채팅방 — 멤버십 = gathering_participants */
	GATHERING,
	/** 일정 채팅방 — 멤버십 = schedule_participants */
	SCHEDULE
}
