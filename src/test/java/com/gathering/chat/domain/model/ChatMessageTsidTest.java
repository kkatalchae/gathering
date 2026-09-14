package com.gathering.chat.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;

/**
 * ChatMessageTsid 테스트 — 시각→TSID 하한이 실제 생성 TSID 와 문자열 순서로 비교 가능한지 검증한다
 */
class ChatMessageTsidTest {

	@Test
	@DisplayName("시각의 하한 TSID 는 그 시각을 그대로 담고, 그 이후에 만들어진 TSID 보다 문자열 순서가 앞선다")
	void floorOfPrecedesLaterTsids() {
		// given
		Instant joinedAt = Instant.now().minusSeconds(1);

		// when
		String floor = ChatMessageTsid.floorOf(joinedAt);
		String createdLater = TsidCreator.getTsid().toString();

		// then
		assertThat(Tsid.from(floor).getInstant().toEpochMilli()).isEqualTo(joinedAt.toEpochMilli());
		assertThat(floor).isLessThan(createdLater).hasSize(Tsid.TSID_CHARS);
	}

	@Test
	@DisplayName("같은 밀리초에 만들어진 TSID 도 하한보다 앞서지 않는다")
	void floorIsMinimumOfItsMillisecond() {
		// given
		Tsid created = TsidCreator.getTsid();

		// when
		String floor = ChatMessageTsid.floorOf(created.getInstant());

		// then
		assertThat(floor).isLessThanOrEqualTo(created.toString());
	}

	@Test
	@DisplayName("TSID epoch(2020-01-01) 이전 시각은 epoch 로 본다")
	void floorOfBeforeEpoch() {
		assertThat(ChatMessageTsid.floorOf(Instant.parse("2019-06-01T00:00:00Z")))
			.isEqualTo(ChatMessageTsid.floorOf(Instant.ofEpochMilli(Tsid.TSID_EPOCH)));
	}

	@Test
	@DisplayName("둘 중 더 나중 TSID 를 고르고 null 은 없는 것으로 본다")
	void later() {
		String earlier = ChatMessageTsid.floorOf(Instant.parse("2026-09-01T00:00:00Z"));
		String later = ChatMessageTsid.floorOf(Instant.parse("2026-09-02T00:00:00Z"));

		assertThat(ChatMessageTsid.later(earlier, later)).isEqualTo(later);
		assertThat(ChatMessageTsid.later(later, earlier)).isEqualTo(later);
		assertThat(ChatMessageTsid.later(null, earlier)).isEqualTo(earlier);
		assertThat(ChatMessageTsid.later(earlier, null)).isEqualTo(earlier);
	}

	@Test
	@DisplayName("13자 Crockford base32 가 아니면 유효하지 않다")
	void isValid() {
		assertThat(ChatMessageTsid.isValid(TsidCreator.getTsid().toString())).isTrue();
		assertThat(ChatMessageTsid.isValid("not-a-tsid")).isFalse();
		assertThat(ChatMessageTsid.isValid(null)).isFalse();
	}
}
