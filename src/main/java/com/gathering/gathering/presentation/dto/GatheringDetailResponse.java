package com.gathering.gathering.presentation.dto;

import java.time.Instant;
import java.util.List;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatheringDetailResponse {

	@NotNull
	private String tsid;

	@NotNull
	private String name;

	private String description;

	private String mainImageUrl;

	@NotNull
	private GatheringCategory category;

	@NotNull
	private String regionName;

	@NotNull
	private List<ParticipantSummary> participants;

	@NotNull
	private Instant createdAt;

	public static GatheringDetailResponse from(
		GatheringEntity gathering,
		List<GatheringParticipantEntity> participants
	) {
		return GatheringDetailResponse.builder()
			.tsid(gathering.getTsid())
			.name(gathering.getName())
			.description(gathering.getDescription())
			.mainImageUrl(gathering.getMainImageUrl())
			.category(gathering.getCategory())
			.regionName(gathering.getRegion().getName())
			.participants(participants.stream()
				.map(ParticipantSummary::from)
				.toList())
			.createdAt(gathering.getCreatedAt())
			.build();
	}
}