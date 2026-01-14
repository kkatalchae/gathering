package com.gathering.gathering.domain.policy;

import org.springframework.stereotype.Component;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
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
	private final GatheringParticipantRepository participantRepository;

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

	/**
	 * 모임 수정 권한 검증
	 * OWNER 또는 ADMIN만 모임 정보를 수정할 수 있음
	 *
	 * @param gatheringTsid 모임 TSID
	 * @param userTsid 요청 사용자 TSID
	 * @throws BusinessException 권한이 없거나 참여자가 아닌 경우
	 */
	public void validateUpdatePermission(String gatheringTsid, String userTsid) {
		GatheringParticipantEntity participant = participantRepository
			.findByGatheringTsidAndUserTsid(gatheringTsid, userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_PERMISSION_DENIED));

		if (participant.getRole() == ParticipantRole.MEMBER) {
			throw new BusinessException(ErrorCode.GATHERING_PERMISSION_DENIED);
		}
	}
}