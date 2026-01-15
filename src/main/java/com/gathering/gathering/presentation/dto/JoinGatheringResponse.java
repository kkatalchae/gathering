package com.gathering.gathering.presentation.dto;

import java.time.Instant;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * 모임 참여 응답 DTO
 */
@Getter
@Builder
public class JoinGatheringResponse {

	@NotNull
	private String participantTsid;
	@NotNull
	private String gatheringTsid;
	@NotNull
	private String userTsid;
	@NotNull
	private ParticipantRole role;
	@NotNull
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
