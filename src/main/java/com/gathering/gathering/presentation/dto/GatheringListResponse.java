package com.gathering.gathering.presentation.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatheringListResponse {

	private List<GatheringListItemResponse> gatherings;

	private String nextCursor;

	private Boolean hasNext;

	public static GatheringListResponse of(
		List<GatheringListItemResponse> gatherings,
		String nextCursor,
		boolean hasNext
	) {
		return GatheringListResponse.builder()
			.gatherings(gatherings)
			.nextCursor(nextCursor)
			.hasNext(hasNext)
			.build();
	}
}