package com.gathering.gathering.presentation.dto;

import java.util.List;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringCategory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringListRequest {

	private List<GatheringCategory> categories;

	private List<String> regionTsids;

	private String cursor;

	@Builder.Default
	private int size = 20;

	public void validate() {
		if (size <= 0 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
		}
	}
}