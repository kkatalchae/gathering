package com.gathering.gathering.domain.policy;

import org.springframework.stereotype.Component;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.region.domain.repository.RegionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 모임 도메인 규칙 검증을 담당하는 Domain Policy
 * DDD 원칙에 따라 도메인 규칙을 캡슐화
 */
@Component
@RequiredArgsConstructor
public class GatheringPolicy {

	private final RegionRepository regionRepository;

	/**
	 * 지역 존재 여부 검증
	 *
	 * @param regionTsid 검증할 지역 TSID
	 * @throws BusinessException 지역이 존재하지 않는 경우
	 */
	public void validateRegionExists(String regionTsid) {
		if (!regionRepository.existsById(regionTsid)) {
			throw new BusinessException(ErrorCode.REGION_NOT_FOUND);
		}
	}
}