package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ScheduleDescription Value Object 테스트
 */
class ScheduleDescriptionTest {

	@Test
	@DisplayName("정상적인 일정 설명으로 Value Object를 생성할 수 있다")
	void createScheduleDescriptionSuccess() {
		// given
		String validDescription = "한강공원에서 함께 러닝합니다";

		// when
		ScheduleDescription description = new ScheduleDescription(validDescription);

		// then
		assertThat(description.getValue()).isEqualTo(validDescription);
	}

	@Test
	@DisplayName("일정 설명은 null이어도 생성할 수 있다")
	void createScheduleDescriptionWithNull() {
		// when
		ScheduleDescription description = new ScheduleDescription(null);

		// then
		assertThat(description.getValue()).isNull();
	}

	@Test
	@DisplayName("일정 설명이 1000자를 초과하면 예외가 발생한다")
	void createScheduleDescriptionTooLong() {
		// given
		String tooLongDescription = "a".repeat(ScheduleDescription.MAX_LENGTH + 1);

		// when & then
		assertThatThrownBy(() -> new ScheduleDescription(tooLongDescription))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_DESCRIPTION_TOO_LONG);
	}

	@Test
	@DisplayName("일정 설명이 정확히 1000자일 때는 성공한다")
	void createScheduleDescriptionWithMaxLength() {
		// given
		String exactDescription = "a".repeat(ScheduleDescription.MAX_LENGTH);

		// when
		ScheduleDescription description = new ScheduleDescription(exactDescription);

		// then
		assertThat(description.getValue()).hasSize(ScheduleDescription.MAX_LENGTH);
	}
}
