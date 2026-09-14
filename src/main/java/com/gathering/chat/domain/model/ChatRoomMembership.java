package com.gathering.chat.domain.model;

import java.time.Instant;

/**
 * 사용자가 속한 채팅방 하나 — 방과, 그 방의 주체(모임/일정)에 참여한 시각
 * 멤버십은 저장하지 않고 조회 시점에 참여자 테이블에서 파생한다 (docs/adr/0004). 방의 주체는 함께 로딩되어 있어야 한다
 *
 * @param room 채팅방 (gathering 또는 schedule 연관이 fetch 되어 있어야 한다)
 * @param joinedAt 주체에 참여한 시각 — 읽음 위치가 없을 때 "안 읽은" 의 하한
 */
public record ChatRoomMembership(ChatRoomEntity room, Instant joinedAt) {

	/** 목록에 표시할 방 이름 — 모임 이름 또는 일정 제목 */
	public String title() {
		return switch (room.getRoomType()) {
			case GATHERING -> room.getGathering().getName();
			case SCHEDULE -> room.getSchedule().getTitle();
		};
	}
}
