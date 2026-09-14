package com.gathering.chat.presentation.dto;

import java.time.Instant;

import com.gathering.chat.domain.model.ChatRoomType;

import lombok.Builder;
import lombok.Getter;

/**
 * 내 채팅방 목록의 방 하나 — 마지막 메시지 미리보기와 안 읽은 수
 */
@Getter
@Builder
public class ChatRoomSummaryResponse {

	/** 표시 가능한 안 읽은 수의 상한. 이보다 많으면 unreadCount 는 이 값이고 unreadCountCapped=true ("99+") */
	public static final int UNREAD_DISPLAY_MAX = 99;

	private String roomTsid;

	private ChatRoomType roomType;

	/** roomType 이 GATHERING 일 때만 값이 있다 */
	private String gatheringTsid;

	/** roomType 이 SCHEDULE 일 때만 값이 있다 */
	private String scheduleTsid;

	/** 모임 이름 또는 일정 제목 */
	private String title;

	private Instant createdAt;

	/** 마지막 메시지 (없으면 null) */
	private ChatMessageResponse lastMessage;

	/** 내 읽음 위치 (한 번도 읽음 처리하지 않았으면 null) */
	private String lastReadMessageTsid;

	/** 안 읽은 메시지 수 (0 ~ UNREAD_DISPLAY_MAX) */
	private int unreadCount;

	/** true 면 실제 안 읽은 수가 unreadCount 보다 많다 — "99+" 로 표시 */
	private boolean unreadCountCapped;
}
