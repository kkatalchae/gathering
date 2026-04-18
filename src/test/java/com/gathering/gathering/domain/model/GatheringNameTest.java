package com.gathering.gathering.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * GatheringName Value Object 테스트
 */
class GatheringNameTest {

	@Test
	@DisplayName("정상적인 모임 이름으로 Value Object를 생성할 수 있다")
	void createGatheringNameSuccess() {
		// given
		String validName = "테스트 모임";

		// when
		GatheringName gatheringName = new GatheringName(validName);

		// then
		assertThat(gatheringName.getValue()).isEqualTo(validName);
	}

	@Test
	@DisplayName("모임 이름이 null이면 예외가 발생한다")
	void createGatheringNameWithNull() {
		// given
		String nullName = null;

		// when & then
		assertThatThrownBy(() -> new GatheringName(nullName))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NAME_REQUIRED);
	}

	@Test
	@DisplayName("모임 이름이 빈 문자열이면 예외가 발생한다")
	void createGatheringNameWithBlank() {
		// given
		String blankName = "   ";

		// when & then
		assertThatThrownBy(() -> new GatheringName(blankName))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NAME_REQUIRED);
	}

	@Test
	@DisplayName("모임 이름이 25자를 초과하면 예외가 발생한다")
	void createGatheringNameTooLong() {
		// given
		String tooLongName = "a".repeat(26);

		// when & then
		assertThatThrownBy(() -> new GatheringName(tooLongName))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NAME_TOO_LONG);
	}

	@Test
	@DisplayName("모임 이름이 정확히 25자일 때는 성공한다")
	void createGatheringNameWithExactly25Characters() {
		// given
		String exactName = "a".repeat(25);

		// when
		GatheringName gatheringName = new GatheringName(exactName);

		// then
		assertThat(gatheringName.getValue()).isEqualTo(exactName);
		assertThat(gatheringName.getValue()).hasSize(25);
	}
}