package com.gathering.gathering.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 모임 이름 Value Object
 * 값 유효성 검증을 담당 (필수, 25자 이하)
 */
@Getter
public class GatheringName {

	private final String value;

	/**
	 * 모임 이름 생성
	 *
	 * @param value 모임 이름
	 * @throws BusinessException 이름이 null/blank이거나 25자를 초과하는 경우
	 */
	public GatheringName(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.GATHERING_NAME_REQUIRED);
		}
		if (value.length() > 25) {
			throw new BusinessException(ErrorCode.GATHERING_NAME_TOO_LONG);
		}
		this.value = value;
	}
}