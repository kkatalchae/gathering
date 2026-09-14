package com.gathering.chat.presentation.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 메시지 목록 응답 DTO
 * before 조회는 최신순, after 조회는 오래된 순으로 정렬된다. nextCursor 는 같은 방향으로 이어서 조회할 때 쓴다
 */
@Getter
@Builder
public class ChatMessageListResponse {

	private List<ChatMessageResponse> messages;

	private String nextCursor;

	private Boolean hasNext;

	public static ChatMessageListResponse of(List<ChatMessageResponse> messages, String nextCursor, boolean hasNext) {
		return ChatMessageListResponse.builder()
			.messages(messages)
			.nextCursor(nextCursor)
			.hasNext(hasNext)
			.build();
	}
}
