package com.gathering.chat.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ChatMessageEntity 도메인 로직 테스트
 */
class ChatMessageEntityTest {

	@Test
	@DisplayName("일반 메시지는 TSID 를 발급받고 그 시각의 버킷에 배정된다")
	void textMessage() {
		// when
		ChatMessageEntity message = ChatMessageEntity.text("01HQROOM000001", "01HQUSER000001", "안녕하세요");

		// then
		assertThat(message.getMessageType()).isEqualTo(ChatMessageType.TEXT);
		assertThat(message.getSenderTsid()).isEqualTo("01HQUSER000001");
		assertThat(message.getRoomTsid()).isEqualTo("01HQROOM000001");
		assertThat(message.getMessageTsid()).hasSize(13);
		assertThat(message.getKey().getBucket())
			.isEqualTo(ChatMessageBucket.ofMessageTsid(message.getMessageTsid()).getValue());
		assertThat(message.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("시스템 메시지는 보낸 사람이 없다")
	void systemMessage() {
		// when
		ChatMessageEntity message = ChatMessageEntity.system("01HQROOM000001", "김병채 님이 입장했습니다");

		// then
		assertThat(message.getMessageType()).isEqualTo(ChatMessageType.SYSTEM);
		assertThat(message.getSenderTsid()).isNull();
	}

	@Test
	@DisplayName("연속으로 만든 메시지의 TSID 는 생성 순서대로 커진다")
	void tsidIsMonotonic() {
		// when
		ChatMessageEntity first = ChatMessageEntity.text("01HQROOM000001", "01HQUSER000001", "1");
		ChatMessageEntity second = ChatMessageEntity.text("01HQROOM000001", "01HQUSER000001", "2");

		// then
		assertThat(second.getMessageTsid()).isGreaterThan(first.getMessageTsid());
	}
}
