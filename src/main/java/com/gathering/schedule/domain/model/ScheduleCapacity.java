package com.gathering.schedule.domain.model;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 일정 정원 Value Object
 * 값이 null이면 인원 제한이 없음을 의미한다
 */
@Getter
public class ScheduleCapacity {

	public static final int MIN_VALUE = 1;

	private final Integer value;

	/**
	 * 일정 정원 생성
	 *
	 * @param value 정원 (null이면 제한 없음)
	 * @throws BusinessException 정원이 1보다 작은 경우
	 */
	public ScheduleCapacity(Integer value) {
		if (value != null && value < MIN_VALUE) {
			throw new BusinessException(ErrorCode.INVALID_SCHEDULE_CAPACITY);
		}
		this.value = value;
	}

	/**
	 * 인원 제한 없음 여부
	 *
	 * @return 정원이 지정되지 않았으면 true
	 */
	public boolean isUnlimited() {
		return value == null;
	}

	/**
	 * 현재 참여 인원이 정원에 도달했는지 여부
	 *
	 * @param currentParticipantCount 현재 참여 인원
	 * @return 정원이 지정되어 있고 현재 인원이 정원 이상이면 true
	 */
	public boolean isFull(long currentParticipantCount) {
		return !isUnlimited() && currentParticipantCount >= value;
	}
}
