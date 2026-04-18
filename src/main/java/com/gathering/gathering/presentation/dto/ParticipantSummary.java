package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantSummary {

	@NotNull
	private String userTsid;

	@NotNull
	private String nickname;

	private String profileImageUrl;

	@NotNull
	private ParticipantRole role;

	public static ParticipantSummary from(GatheringParticipantEntity participant) {
		return ParticipantSummary.builder()
			.userTsid(participant.getUserTsid())
			.nickname(participant.getUser().getNickname())
			.profileImageUrl(participant.getUser().getProfileImageUrl())
			.role(participant.getRole())
			.build();
	}
}