package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatheringListItemResponse {

	@NotNull
	private String tsid;

	@NotNull
	private String name;

	private String mainImageUrl;

	@NotNull
	private GatheringCategory category;

	@NotNull
	private String regionName;

	public static GatheringListItemResponse from(GatheringEntity gathering) {
		return GatheringListItemResponse.builder()
			.tsid(gathering.getTsid())
			.name(gathering.getName())
			.mainImageUrl(gathering.getMainImageUrl())
			.category(gathering.getCategory())
			.regionName(gathering.getRegion().getName())
			.build();
	}
}