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

	private String nickname;

	@NotNull
	private String name;

	private String profileImageUrl;

	@NotNull
	private ParticipantRole role;

	public static ParticipantSummary from(GatheringParticipantEntity participant) {
		return ParticipantSummary.builder()
			.userTsid(participant.getUserTsid())
			.nickname(participant.getUser().getNickname())
			.name(participant.getUser().getName())
			.profileImageUrl(participant.getUser().getProfileImageUrl())
			.role(participant.getRole())
			.build();
	}
}