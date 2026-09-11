package com.gathering.schedule.presentation.dto;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.schedule.domain.model.ScheduleTimeFilter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 목록 조회 요청 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleListRequest {

	/** null이면 모임 귀속 여부와 무관하게 전체 일정 조회 */
	private String gatheringTsid;

	@Builder.Default
	private ScheduleTimeFilter timeFilter = ScheduleTimeFilter.UPCOMING;

	private String cursor;

	@Builder.Default
	private int size = 20;

	public void validate() {
		if (size <= 0 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
		}
	}
}
