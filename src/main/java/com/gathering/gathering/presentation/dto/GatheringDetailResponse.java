package com.gathering.gathering.presentation.dto;

import java.time.Instant;
import java.util.List;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatheringDetailResponse {

	private String tsid;

	private String name;

	private String description;

	private String mainImageUrl;

	private GatheringCategory category;

	private String regionTsid;

	private String regionName;

	private List<ParticipantSummary> participants;

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
			.regionTsid(gathering.getRegionTsid())
			.regionName(gathering.getRegion().getName())
			.participants(participants.stream()
				.map(ParticipantSummary::from)
				.toList())
			.createdAt(gathering.getCreatedAt())
			.build();
	}
}