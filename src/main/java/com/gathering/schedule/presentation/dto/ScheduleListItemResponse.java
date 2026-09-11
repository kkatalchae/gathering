package com.gathering.schedule.presentation.dto;

import java.time.Instant;

import com.gathering.schedule.domain.model.ScheduleEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 목록 항목 응답 DTO
 */
@Getter
@Builder
public class ScheduleListItemResponse {

	private String tsid;

	/** 모임에 귀속되지 않은 독립 일정이면 null */
	private String gatheringTsid;

	/** 모임에 귀속되지 않은 독립 일정이면 null */
	private String gatheringName;

	private String title;

	private Instant startAt;

	private Instant endAt;

	private String locationName;

	/** null이면 인원 제한 없음 */
	private Integer maxParticipants;

	private long participantCount;

	private String hostTsid;

	private String hostNickname;

	public static ScheduleListItemResponse from(ScheduleEntity schedule, long participantCount) {
		return ScheduleListItemResponse.builder()
			.tsid(schedule.getTsid())
			.gatheringTsid(schedule.getGatheringTsid())
			.gatheringName(schedule.isAttachedToGathering() ? schedule.getGathering().getName() : null)
			.title(schedule.getTitle())
			.startAt(schedule.getStartAt())
			.endAt(schedule.getEndAt())
			.locationName(schedule.getLocationName())
			.maxParticipants(schedule.getMaxParticipants())
			.participantCount(participantCount)
			.hostTsid(schedule.getCreatedBy())
			.hostNickname(schedule.getCreator().getNickname())
			.build();
	}
}
