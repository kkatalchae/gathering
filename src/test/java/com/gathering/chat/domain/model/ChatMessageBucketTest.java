package com.gathering.chat.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.f4b6a3.tsid.TsidCreator;

/**
 * ChatMessageBucket Value Object 테스트
 */
class ChatMessageBucketTest {

	@Test
	@DisplayName("시각을 UTC 기준 yyyy-MM 버킷으로 바꾼다")
	void ofInstant() {
		// given: KST 로는 10월 1일 새벽이지만 UTC 로는 9월 30일
		Instant instant = Instant.parse("2026-09-30T16:30:00Z");

		// when & then
		assertThat(ChatMessageBucket.of(instant).getValue()).isEqualTo("2026-09");
	}

	@Test
	@DisplayName("메시지 TSID 에 담긴 시각으로 버킷을 복원한다")
	void ofMessageTsid() {
		// given
		String messageTsid = TsidCreator.getTsid().toString();

		// when & then
		assertThat(ChatMessageBucket.ofMessageTsid(messageTsid))
			.isEqualTo(ChatMessageBucket.of(Instant.now()));
	}

	@Test
	@DisplayName("이전 버킷은 한 달 전이며 연도 경계를 넘는다")
	void previous() {
		// given
		ChatMessageBucket january = ChatMessageBucket.parse("2026-01");

		// when
		ChatMessageBucket previous = january.previous();

		// then
		assertThat(previous.getValue()).isEqualTo("2025-12");
		assertThat(previous.isBefore(january)).isTrue();
		assertThat(january.isBefore(previous)).isFalse();
	}

	@Test
	@DisplayName("문자열로 파싱한 버킷과 시각으로 만든 버킷은 같은 값이다")
	void parseEqualsOf() {
		// when & then
		assertThat(ChatMessageBucket.parse("2026-09"))
			.isEqualTo(ChatMessageBucket.of(Instant.parse("2026-09-15T00:00:00Z")));
	}
}
