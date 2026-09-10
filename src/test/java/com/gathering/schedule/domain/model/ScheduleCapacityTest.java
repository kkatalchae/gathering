package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ScheduleCapacity Value Object 테스트
 */
class ScheduleCapacityTest {

	@Test
	@DisplayName("정원을 지정해 Value Object를 생성할 수 있다")
	void createScheduleCapacitySuccess() {
		// when
		ScheduleCapacity capacity = new ScheduleCapacity(4);

		// then
		assertThat(capacity.getValue()).isEqualTo(4);
		assertThat(capacity.isUnlimited()).isFalse();
	}

	@Test
	@DisplayName("정원이 null이면 인원 제한이 없는 일정으로 판단한다")
	void createScheduleCapacityWithNull() {
		// when
		ScheduleCapacity capacity = new ScheduleCapacity(null);

		// then
		assertThat(capacity.isUnlimited()).isTrue();
		assertThat(capacity.isFull(Long.MAX_VALUE)).isFalse();
	}

	@Test
	@DisplayName("정원이 1보다 작으면 예외가 발생한다")
	void createScheduleCapacityBelowMinimum() {
		// when & then
		assertThatThrownBy(() -> new ScheduleCapacity(0))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SCHEDULE_CAPACITY);
	}

	@Test
	@DisplayName("현재 인원이 정원에 도달하면 정원이 찬 것으로 판단한다")
	void isFullWhenCurrentCountReachesCapacity() {
		// given
		ScheduleCapacity capacity = new ScheduleCapacity(4);

		// when & then
		assertThat(capacity.isFull(3)).isFalse();
		assertThat(capacity.isFull(4)).isTrue();
		assertThat(capacity.isFull(5)).isTrue();
	}
}
