package com.gathering.chat.presentation.dto;

import java.time.Instant;

import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 읽음 위치 응답 DTO
 */
@Getter
@Builder
public class ChatReadPositionResponse {

	private String roomTsid;

	private String lastReadMessageTsid;

	private Instant updatedAt;

	public static ChatReadPositionResponse from(ChatRoomReadPositionEntity position) {
		return ChatReadPositionResponse.builder()
			.roomTsid(position.getRoomTsid())
			.lastReadMessageTsid(position.getLastReadMessageTsid())
			.updatedAt(position.getUpdatedAt())
			.build();
	}
}
