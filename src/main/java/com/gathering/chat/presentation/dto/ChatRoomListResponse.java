package com.gathering.chat.presentation.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내 채팅방 목록 응답 — 마지막 메시지가 최근인 방부터, 메시지가 없는 방은 뒤에 생성 역순
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomListResponse {

	private List<ChatRoomSummaryResponse> rooms;

	public static ChatRoomListResponse of(List<ChatRoomSummaryResponse> rooms) {
		return new ChatRoomListResponse(rooms);
	}
}
