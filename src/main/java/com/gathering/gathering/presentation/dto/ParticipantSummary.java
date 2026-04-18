package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantSummary {

	private String userTsid;

	private String nickname;

	private String name;

	private String profileImageUrl;

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