package com.gathering.gathering.presentation.dto;

import java.time.Instant;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * 모임 생성 응답 DTO
 */
@Getter
@Builder
public class CreateGatheringResponse {

	@NotNull
	private String tsid;
	@NotNull
	private String name;
	private String description;
	@NotNull
	private String regionTsid;
	@NotNull
	private GatheringCategory category;
	private String mainImageUrl;
	@NotNull
	private Integer maxParticipants;
	@NotNull
	private Instant createdAt;

	/**
	 * Entity로부터 응답 DTO 생성
	 *
	 * @param gathering 모임 엔티티
	 * @return 모임 생성 응답 DTO
	 */
	public static CreateGatheringResponse from(GatheringEntity gathering) {
		return CreateGatheringResponse.builder()
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
