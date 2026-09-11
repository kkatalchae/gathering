package com.gathering.schedule.presentation.dto;

import java.time.Instant;
import java.util.List;

import com.gathering.schedule.domain.model.ScheduleEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 상세 조회 응답 DTO
 */
@Getter
@Builder
public class ScheduleDetailResponse {

	private String tsid;

	/** 모임에 귀속되지 않은 독립 일정이면 null */
	private String gatheringTsid;

	/** 모임에 귀속되지 않은 독립 일정이면 null */
	private String gatheringName;

	private String title;

	private String description;

	private Instant startAt;

	private Instant endAt;

	private String locationName;

	private String locationAddress;

	/** null이면 인원 제한 없음 */
	private Integer maxParticipants;

	/** 참여 순으로 정렬된 전체 참여자 (호스트 포함) */
	private List<ScheduleParticipantSummary> participants;

	private String hostTsid;

	private String hostNickname;

	private String hostName;

	private String hostProfileImageUrl;

	private Instant createdAt;

	private Instant updatedAt;

	public static ScheduleDetailResponse from(ScheduleEntity schedule, List<ScheduleParticipantSummary> participants) {
		return ScheduleDetailResponse.builder()
			.tsid(schedule.getTsid())
			.gatheringTsid(schedule.getGatheringTsid())
			.gatheringName(schedule.isAttachedToGathering() ? schedule.getGathering().getName() : null)
			.title(schedule.getTitle())
			.description(schedule.getDescription())
			.startAt(schedule.getStartAt())
			.endAt(schedule.getEndAt())
			.locationName(schedule.getLocationName())
			.locationAddress(schedule.getLocationAddress())
			.maxParticipants(schedule.getMaxParticipants())
			.participants(participants)
			.hostTsid(schedule.getCreatedBy())
			.hostNickname(schedule.getCreator().getNickname())
			.hostName(schedule.getCreator().getName())
			.hostProfileImageUrl(schedule.getCreator().getProfileImageUrl())
			.createdAt(schedule.getCreatedAt())
			.updatedAt(schedule.getUpdatedAt())
			.build();
	}
}
