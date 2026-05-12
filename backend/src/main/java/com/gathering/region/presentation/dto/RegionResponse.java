package com.gathering.region.presentation.dto;

import java.util.List;

import com.gathering.region.domain.model.RegionEntity;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegionResponse {

	@NotNull
	private String tsid;

	@NotNull
	private String code;

	@NotNull
	private String name;

	@NotNull
	private Integer depth;

	private List<RegionResponse> children;

	public static RegionResponse from(RegionEntity entity, List<RegionResponse> children) {
		return RegionResponse.builder()
			.tsid(entity.getTsid())
			.code(entity.getCode())
			.name(entity.getName())
			.depth(entity.getDepth())
			.children(children)
			.build();
	}

	public static RegionResponse fromLeaf(RegionEntity entity) {
		return from(entity, List.of());
	}
}