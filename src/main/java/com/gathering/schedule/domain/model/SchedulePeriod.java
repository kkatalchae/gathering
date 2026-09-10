package com.gathering.schedule.domain.model;

import java.time.Instant;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 일정 기간 Value Object
 * 시작 시각은 필수이며, 종료 시각이 있으면 시작 시각보다 이후여야 한다
 */
@Getter
public class SchedulePeriod {

	private final Instant startAt;
	private final Instant endAt;

	/**
	 * 일정 기간 생성
	 *
	 * @param startAt 시작 시각
	 * @param endAt 종료 시각 (null 허용)
	 * @throws BusinessException 시작 시각이 없거나 종료 시각이 시작 시각보다 앞선 경우
	 */
	public SchedulePeriod(Instant startAt, Instant endAt) {
		if (startAt == null) {
			throw new BusinessException(ErrorCode.SCHEDULE_START_AT_REQUIRED);
		}
		if (endAt != null && !endAt.isAfter(startAt)) {
			throw new BusinessException(ErrorCode.SCHEDULE_END_AT_BEFORE_START_AT);
		}
		this.startAt = startAt;
		this.endAt = endAt;
	}

	/**
	 * 기준 시각에 이미 시작된 일정인지 여부
	 *
	 * @param baseTime 기준 시각
	 * @return 시작 시각이 기준 시각보다 앞서면 true
	 */
	public boolean isStartedBefore(Instant baseTime) {
		return startAt.isBefore(baseTime);
	}
}
