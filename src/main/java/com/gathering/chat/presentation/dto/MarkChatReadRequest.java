package com.gathering.chat.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽음 위치 갱신 요청 DTO
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarkChatReadRequest {

	@NotBlank(message = "마지막으로 읽은 메시지 TSID 는 필수입니다")
	private String lastReadMessageTsid;
}
