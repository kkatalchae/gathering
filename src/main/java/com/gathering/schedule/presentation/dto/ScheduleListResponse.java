package com.gathering.schedule.presentation.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 목록 조회 응답 DTO
 */
@Getter
@Builder
public class ScheduleListResponse {

	private List<ScheduleListItemResponse> schedules;

	private String nextCursor;

	private Boolean hasNext;

	public static ScheduleListResponse of(List<ScheduleListItemResponse> schedules, String nextCursor,
		boolean hasNext) {
		return ScheduleListResponse.builder()
			.schedules(schedules)
			.nextCursor(nextCursor)
			.hasNext(hasNext)
			.build();
	}
}
