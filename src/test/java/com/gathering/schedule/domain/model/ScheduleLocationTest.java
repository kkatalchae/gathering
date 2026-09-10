package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ScheduleLocation Value Object 테스트
 */
class ScheduleLocationTest {

	@Test
	@DisplayName("장소명과 주소로 Value Object를 생성할 수 있다")
	void createScheduleLocationSuccess() {
		// given
		String name = "한강공원 2주차장";
		String address = "서울특별시 영등포구 여의동로 330";

		// when
		ScheduleLocation location = new ScheduleLocation(name, address);

		// then
		assertThat(location.getName()).isEqualTo(name);
		assertThat(location.getAddress()).isEqualTo(address);
	}

	@Test
	@DisplayName("상세 주소 없이 장소명만으로도 생성할 수 있다")
	void createScheduleLocationWithoutAddress() {
		// when
		ScheduleLocation location = new ScheduleLocation("한강공원 2주차장", null);

		// then
		assertThat(location.getAddress()).isNull();
	}

	@Test
	@DisplayName("장소명이 null이면 예외가 발생한다")
	void createScheduleLocationWithNullName() {
		// when & then
		assertThatThrownBy(() -> new ScheduleLocation(null, "서울특별시 영등포구 여의동로 330"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_LOCATION_NAME_REQUIRED);
	}

	@Test
	@DisplayName("장소명이 빈 문자열이면 예외가 발생한다")
	void createScheduleLocationWithBlankName() {
		// when & then
		assertThatThrownBy(() -> new ScheduleLocation("   ", null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_LOCATION_NAME_REQUIRED);
	}

	@Test
	@DisplayName("장소명이 100자를 초과하면 예외가 발생한다")
	void createScheduleLocationWithTooLongName() {
		// given
		String tooLongName = "a".repeat(ScheduleLocation.MAX_NAME_LENGTH + 1);

		// when & then
		assertThatThrownBy(() -> new ScheduleLocation(tooLongName, null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_LOCATION_NAME_TOO_LONG);
	}

	@Test
	@DisplayName("장소 주소가 255자를 초과하면 예외가 발생한다")
	void createScheduleLocationWithTooLongAddress() {
		// given
		String tooLongAddress = "a".repeat(ScheduleLocation.MAX_ADDRESS_LENGTH + 1);

		// when & then
		assertThatThrownBy(() -> new ScheduleLocation("한강공원 2주차장", tooLongAddress))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_LOCATION_ADDRESS_TOO_LONG);
	}
}
