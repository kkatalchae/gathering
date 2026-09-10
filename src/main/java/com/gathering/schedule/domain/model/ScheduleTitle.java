package com.gathering.schedule.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 일정 제목 Value Object
 * 값 유효성 검증을 담당 (필수, 50자 이하)
 */
@Getter
public class ScheduleTitle {

	public static final int MAX_LENGTH = 50;

	private final String value;

	/**
	 * 일정 제목 생성
	 *
	 * @param value 일정 제목
	 * @throws BusinessException 제목이 null/blank이거나 50자를 초과하는 경우
	 */
	public ScheduleTitle(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.SCHEDULE_TITLE_REQUIRED);
		}
		if (value.length() > MAX_LENGTH) {
			throw new BusinessException(ErrorCode.SCHEDULE_TITLE_TOO_LONG);
		}
		this.value = value;
	}
}
