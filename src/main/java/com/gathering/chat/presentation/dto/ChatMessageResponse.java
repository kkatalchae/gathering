package com.gathering.chat.presentation.dto;

import java.time.Instant;

import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatMessageType;

import lombok.Builder;
import lombok.Getter;

/**
 * 채팅 메시지 응답 DTO — REST 조회와 실시간 브로드캐스트가 같은 형태를 쓴다
 */
@Getter
@Builder
public class ChatMessageResponse {

	private String messageTsid;

	private String roomTsid;

	private ChatMessageType messageType;

	/** SYSTEM 메시지는 발신자가 없어 null */
	private ChatSenderSummary sender;

	private String content;

	private Instant createdAt;

	public static ChatMessageResponse from(ChatMessageEntity message, ChatSenderSummary sender) {
		return ChatMessageResponse.builder()
			.messageTsid(message.getMessageTsid())
			.roomTsid(message.getRoomTsid())
			.messageType(message.getMessageType())
			.sender(sender)
			.content(message.getContent())
			.createdAt(message.getCreatedAt())
			.build();
	}
}
