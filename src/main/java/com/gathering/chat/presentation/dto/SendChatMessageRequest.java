package com.gathering.chat.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메시지 전송 요청 DTO
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SendChatMessageRequest {

	@NotBlank(message = "메시지 내용은 필수입니다")
	private String content;
}
