package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.GatheringCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모임 수정 요청 DTO
 * PUT 방식으로 전체 교체 (모든 필드를 매번 전송)
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateGatheringRequest {

	@NotBlank(message = "모임 이름은 필수입니다")
	private String name;

	private String description;

	@NotBlank(message = "지역은 필수입니다")
	private String regionTsid;

	@NotNull(message = "카테고리는 필수입니다")
	private GatheringCategory category;

	private String mainImageUrl;
}
