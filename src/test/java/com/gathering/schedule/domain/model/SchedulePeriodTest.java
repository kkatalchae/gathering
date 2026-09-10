package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * SchedulePeriod Value Object 테스트
 */
class SchedulePeriodTest {

	@Test
	@DisplayName("시작 시각과 종료 시각으로 Value Object를 생성할 수 있다")
	void createSchedulePeriodSuccess() {
		// given
		Instant startAt = Instant.parse("2026-09-20T10:00:00Z");
		Instant endAt = startAt.plus(Duration.ofHours(2));

		// when
		SchedulePeriod period = new SchedulePeriod(startAt, endAt);

		// then
		assertThat(period.getStartAt()).isEqualTo(startAt);
		assertThat(period.getEndAt()).isEqualTo(endAt);
	}

	@Test
	@DisplayName("종료 시각은 null이어도 생성할 수 있다")
	void createSchedulePeriodWithoutEndAt() {
		// given
		Instant startAt = Instant.parse("2026-09-20T10:00:00Z");

		// when
		SchedulePeriod period = new SchedulePeriod(startAt, null);

		// then
		assertThat(period.getEndAt()).isNull();
	}

	@Test
	@DisplayName("시작 시각이 null이면 예외가 발생한다")
	void createSchedulePeriodWithoutStartAt() {
		// when & then
		assertThatThrownBy(() -> new SchedulePeriod(null, Instant.parse("2026-09-20T10:00:00Z")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_START_AT_REQUIRED);
	}

	@Test
	@DisplayName("종료 시각이 시작 시각보다 앞서면 예외가 발생한다")
	void createSchedulePeriodWithEndAtBeforeStartAt() {
		// given
		Instant startAt = Instant.parse("2026-09-20T10:00:00Z");
		Instant endAt = startAt.minus(Duration.ofMinutes(1));

		// when & then
		assertThatThrownBy(() -> new SchedulePeriod(startAt, endAt))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_END_AT_BEFORE_START_AT);
	}

	@Test
	@DisplayName("종료 시각이 시작 시각과 같으면 예외가 발생한다")
	void createSchedulePeriodWithSameStartAtAndEndAt() {
		// given
		Instant startAt = Instant.parse("2026-09-20T10:00:00Z");

		// when & then
		assertThatThrownBy(() -> new SchedulePeriod(startAt, startAt))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_END_AT_BEFORE_START_AT);
	}

	@Test
	@DisplayName("기준 시각보다 시작 시각이 앞서면 이미 시작된 일정으로 판단한다")
	void isStartedBefore() {
		// given
		Instant baseTime = Instant.parse("2026-09-20T10:00:00Z");
		SchedulePeriod startedPeriod = new SchedulePeriod(baseTime.minus(Duration.ofHours(1)), null);
		SchedulePeriod upcomingPeriod = new SchedulePeriod(baseTime.plus(Duration.ofHours(1)), null);

		// when & then
		assertThat(startedPeriod.isStartedBefore(baseTime)).isTrue();
		assertThat(upcomingPeriod.isStartedBefore(baseTime)).isFalse();
	}
}
