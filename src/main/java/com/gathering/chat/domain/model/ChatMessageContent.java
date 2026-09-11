package com.gathering.chat.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 채팅 메시지 내용 Value Object
 * 값 유효성 검증을 담당 (필수, 1000자 이하)
 */
@Getter
public class ChatMessageContent {

	public static final int MAX_LENGTH = 1000;

	private final String value;

	/**
	 * @param value 메시지 내용
	 * @throws BusinessException 내용이 null/blank 이거나 1000자를 초과하는 경우
	 */
	public ChatMessageContent(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
		}
		if (value.length() > MAX_LENGTH) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
		}
		this.value = value;
	}
}
