package com.gathering.schedule.presentation.dto;

import java.time.Instant;

import com.gathering.schedule.domain.model.ScheduleEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 생성/수정 응답 DTO
 */
@Getter
@Builder
public class ScheduleResponse {

	private String tsid;

	/** 모임에 귀속되지 않은 독립 일정이면 null */
	private String gatheringTsid;

	private String title;

	private String description;

	private Instant startAt;

	private Instant endAt;

	private String locationName;

	private String locationAddress;

	/** null이면 인원 제한 없음 */
	private Integer maxParticipants;

	private String hostTsid;

	private Instant createdAt;

	private Instant updatedAt;

	public static ScheduleResponse from(ScheduleEntity schedule) {
		return ScheduleResponse.builder()
			.tsid(schedule.getTsid())
			.gatheringTsid(schedule.getGatheringTsid())
			.title(schedule.getTitle())
			.description(schedule.getDescription())
			.startAt(schedule.getStartAt())
			.endAt(schedule.getEndAt())
			.locationName(schedule.getLocationName())
			.locationAddress(schedule.getLocationAddress())
			.maxParticipants(schedule.getMaxParticipants())
			.hostTsid(schedule.getCreatedBy())
			.createdAt(schedule.getCreatedAt())
			.updatedAt(schedule.getUpdatedAt())
			.build();
	}
}
