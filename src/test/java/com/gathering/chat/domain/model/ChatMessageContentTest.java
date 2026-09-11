package com.gathering.chat.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ChatMessageContent Value Object 테스트
 */
class ChatMessageContentTest {

	@Test
	@DisplayName("정상적인 내용으로 Value Object를 생성할 수 있다")
	void createSuccess() {
		// when
		ChatMessageContent content = new ChatMessageContent("이번 주 토요일 10시 어때요?");

		// then
		assertThat(content.getValue()).isEqualTo("이번 주 토요일 10시 어때요?");
	}

	@Test
	@DisplayName("내용이 null 이거나 공백뿐이면 예외가 발생한다")
	void createWithBlank() {
		// when & then
		assertThatThrownBy(() -> new ChatMessageContent(null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
		assertThatThrownBy(() -> new ChatMessageContent("   "))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
	}

	@Test
	@DisplayName("내용이 1000자를 초과하면 예외가 발생한다")
	void createTooLong() {
		// when & then
		assertThatThrownBy(() -> new ChatMessageContent("a".repeat(ChatMessageContent.MAX_LENGTH + 1)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
	}
}
