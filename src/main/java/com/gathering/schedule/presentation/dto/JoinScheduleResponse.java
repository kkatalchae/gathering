package com.gathering.schedule.presentation.dto;

import java.time.Instant;

import com.gathering.schedule.domain.model.ScheduleParticipantEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 참여 응답 DTO
 */
@Getter
@Builder
public class JoinScheduleResponse {

	private String participantTsid;
	private String scheduleTsid;
	private String userTsid;
	private Instant joinedAt;

	public static JoinScheduleResponse from(ScheduleParticipantEntity participant) {
		return JoinScheduleResponse.builder()
			.participantTsid(participant.getTsid())
			.scheduleTsid(participant.getScheduleTsid())
			.userTsid(participant.getUserTsid())
			.joinedAt(participant.getJoinedAt())
			.build();
	}
}
