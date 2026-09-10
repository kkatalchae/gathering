package com.gathering.schedule.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 일정 설명 Value Object
 * 값 유효성 검증을 담당 (1000자 이하, nullable)
 */
@Getter
public class ScheduleDescription {

	public static final int MAX_LENGTH = 1000;

	private final String value;

	/**
	 * 일정 설명 생성
	 *
	 * @param value 일정 설명 (null 허용)
	 * @throws BusinessException 설명이 1000자를 초과하는 경우
	 */
	public ScheduleDescription(String value) {
		if (value != null && value.length() > MAX_LENGTH) {
			throw new BusinessException(ErrorCode.SCHEDULE_DESCRIPTION_TOO_LONG);
		}
		this.value = value;
	}
}
