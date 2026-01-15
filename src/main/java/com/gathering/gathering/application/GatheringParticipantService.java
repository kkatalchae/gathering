package com.gathering.gathering.application;

import org.springframework.stereotype.Service;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GatheringParticipantService {

	private final GatheringParticipantRepository participantRepository;

	/**
	 * 역할 변경 대상 참여자 검증
	 *
	 * @param gatheringTsid 모임 TSID
	 * @param targetUserTsid 대상 사용자 TSID
	 * @return 대상 Participant 엔티티
	 * @throws BusinessException 대상이 모임 참여자가 아닌 경우
	 */
	public GatheringParticipantEntity findParticipants(String gatheringTsid, String targetUserTsid) {
		return participantRepository
			.findByGatheringTsidAndUserTsid(gatheringTsid, targetUserTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
	}
}
