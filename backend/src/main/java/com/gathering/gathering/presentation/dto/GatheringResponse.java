package com.gathering.gathering.presentation.dto;

import java.time.Instant;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 모임 수정 응답 DTO
 */
@Getter
@Builder
public class GatheringResponse {

	private String tsid;
	private String name;
	private String description;
	private String regionTsid;
	private GatheringCategory category;
	private String mainImageUrl;
	private Integer maxParticipants;
	private Instant createdAt;

	/**
	 * Entity로부터 응답 DTO 생성
	 *
	 * @param gathering 모임 엔티티
	 * @return 모임 수정 응답 DTO
	 */
	public static GatheringResponse from(GatheringEntity gathering) {
		return GatheringResponse.builder()
			.tsid(gathering.getTsid())
			.name(gathering.getName())
			.description(gathering.getDescription())
			.regionTsid(gathering.getRegionTsid())
			.category(gathering.getCategory())
			.mainImageUrl(gathering.getMainImageUrl())
			.maxParticipants(gathering.getMaxParticipants())
			.createdAt(gathering.getCreatedAt())
			.build();
	}
}
