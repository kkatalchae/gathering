package com.gathering.gathering.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 모임 설명 Value Object
 * 값 유효성 검증을 담당 (1000자 이하, nullable)
 */
@Getter
public class GatheringDescription {

	private final String value;

	/**
	 * 모임 설명 생성
	 *
	 * @param value 모임 설명 (null 허용)
	 * @throws BusinessException 설명이 1000자를 초과하는 경우
	 */
	public GatheringDescription(String value) {
		if (value != null && value.length() > 1000) {
			throw new BusinessException(ErrorCode.GATHERING_DESCRIPTION_TOO_LONG);
		}
		this.value = value;
	}
}