package com.gathering.gathering.domain.policy;

import java.util.Optional;

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

	public void validateRegionExists(String regionTsid) {
		if (!regionRepository.existsById(regionTsid)) {
			throw new BusinessException(ErrorCode.REGION_NOT_FOUND);
		}
	}

	public void validateUpdatePermission(String gatheringTsid, String userTsid) {
		GatheringParticipantEntity participant = findParticipant(gatheringTsid, userTsid);
		if (participant.getRole() == ParticipantRole.MEMBER) {
			throw new BusinessException(ErrorCode.GATHERING_PERMISSION_DENIED);
		}
	}

	public GatheringParticipantEntity validateOwnerPermission(String gatheringTsid, String userTsid) {
		GatheringParticipantEntity participant = findParticipant(gatheringTsid, userTsid);
		if (participant.getRole() != ParticipantRole.OWNER) {
			throw new BusinessException(ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED);
		}
		return participant;
	}

	private GatheringParticipantEntity findParticipant(String gatheringTsid, String userTsid) {
		return participantRepository
			.findByGatheringTsidAndUserTsid(gatheringTsid, userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
	}
}
