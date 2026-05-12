package com.gathering.gathering.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * GatheringDescription Value Object 테스트
 */
class GatheringDescriptionTest {

	@Test
	@DisplayName("정상적인 모임 설명으로 Value Object를 생성할 수 있다")
	void createGatheringDescriptionSuccess() {
		// given
		String validDescription = "이것은 테스트 모임 설명입니다.";

		// when
		GatheringDescription description = new GatheringDescription(validDescription);

		// then
		assertThat(description.getValue()).isEqualTo(validDescription);
	}

	@Test
	@DisplayName("모임 설명이 null이어도 생성할 수 있다")
	void createGatheringDescriptionWithNull() {
		// given
		String nullDescription = null;

		// when
		GatheringDescription description = new GatheringDescription(nullDescription);

		// then
		assertThat(description.getValue()).isNull();
	}

	@Test
	@DisplayName("모임 설명이 1000자를 초과하면 예외가 발생한다")
	void createGatheringDescriptionTooLong() {
		// given
		String tooLongDescription = "a".repeat(1001);

		// when & then
		assertThatThrownBy(() -> new GatheringDescription(tooLongDescription))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_DESCRIPTION_TOO_LONG);
	}

	@Test
	@DisplayName("모임 설명이 정확히 1000자일 때는 성공한다")
	void createGatheringDescriptionWithExactly1000Characters() {
		// given
		String exactDescription = "a".repeat(1000);

		// when
		GatheringDescription description = new GatheringDescription(exactDescription);

		// then
		assertThat(description.getValue()).isEqualTo(exactDescription);
		assertThat(description.getValue()).hasSize(1000);
	}
}