package com.gathering.gathering.presentation.dto;

import java.time.Instant;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;

import lombok.Builder;
import lombok.Getter;

/**
 * 모임 참여 응답 DTO
 */
@Getter
@Builder
public class JoinGatheringResponse {

	private String participantTsid;
	private String gatheringTsid;
	private String userTsid;
	private ParticipantRole role;
	private Instant joinedAt;

	public static JoinGatheringResponse from(GatheringParticipantEntity participant) {
		return JoinGatheringResponse.builder()
			.participantTsid(participant.getTsid())
			.gatheringTsid(participant.getGatheringTsid())
			.userTsid(participant.getUserTsid())
			.role(participant.getRole())
			.joinedAt(participant.getJoinedAt())
			.build();
	}
}
