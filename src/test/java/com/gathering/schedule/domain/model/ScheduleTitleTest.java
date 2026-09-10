package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ScheduleTitle Value Object 테스트
 */
class ScheduleTitleTest {

	@Test
	@DisplayName("정상적인 일정 제목으로 Value Object를 생성할 수 있다")
	void createScheduleTitleSuccess() {
		// given
		String validTitle = "9월 정기 모임";

		// when
		ScheduleTitle title = new ScheduleTitle(validTitle);

		// then
		assertThat(title.getValue()).isEqualTo(validTitle);
	}

	@Test
	@DisplayName("일정 제목이 null이면 예외가 발생한다")
	void createScheduleTitleWithNull() {
		// when & then
		assertThatThrownBy(() -> new ScheduleTitle(null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_TITLE_REQUIRED);
	}

	@Test
	@DisplayName("일정 제목이 빈 문자열이면 예외가 발생한다")
	void createScheduleTitleWithBlank() {
		// when & then
		assertThatThrownBy(() -> new ScheduleTitle("   "))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_TITLE_REQUIRED);
	}

	@Test
	@DisplayName("일정 제목이 50자를 초과하면 예외가 발생한다")
	void createScheduleTitleTooLong() {
		// given
		String tooLongTitle = "a".repeat(ScheduleTitle.MAX_LENGTH + 1);

		// when & then
		assertThatThrownBy(() -> new ScheduleTitle(tooLongTitle))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_TITLE_TOO_LONG);
	}

	@Test
	@DisplayName("일정 제목이 정확히 50자일 때는 성공한다")
	void createScheduleTitleWithMaxLength() {
		// given
		String exactTitle = "a".repeat(ScheduleTitle.MAX_LENGTH);

		// when
		ScheduleTitle title = new ScheduleTitle(exactTitle);

		// then
		assertThat(title.getValue()).hasSize(ScheduleTitle.MAX_LENGTH);
	}
}
