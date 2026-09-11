package com.gathering.schedule.domain.policy;

import org.springframework.stereotype.Component;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.schedule.domain.model.ScheduleEntity;

import lombok.RequiredArgsConstructor;

/**
 * 일정 도메인 규칙 검증을 담당하는 Domain Policy
 */
@Component
@RequiredArgsConstructor
public class SchedulePolicy {

	private final GatheringRepository gatheringRepository;
	private final GatheringParticipantRepository gatheringParticipantRepository;

	/**
	 * 모임 귀속 일정 개설 권한 검증
	 * 모임이 존재하고 요청자가 그 모임의 참여자여야 한다 (역할 무관)
	 *
	 * @param gatheringTsid 모임 TSID
	 * @param userTsid 요청자 TSID
	 * @throws BusinessException 모임이 없거나 요청자가 모임 참여자가 아닌 경우
	 */
	public void validateGatheringParticipant(String gatheringTsid, String userTsid) {
		if (!gatheringRepository.existsById(gatheringTsid)) {
			throw new BusinessException(ErrorCode.GATHERING_NOT_FOUND);
		}
		if (!gatheringParticipantRepository.existsByGatheringTsidAndUserTsid(gatheringTsid, userTsid)) {
			throw new BusinessException(ErrorCode.NOT_GATHERING_PARTICIPANT);
		}
	}

	/**
	 * 일정 관리(수정/삭제) 권한 검증
	 * 일정의 주인은 개설한 호스트이며, 귀속 모임의 OWNER/ADMIN이라도 남의 일정은 관리할 수 없다
	 *
	 * @param schedule 대상 일정
	 * @param userTsid 요청자 TSID
	 * @throws BusinessException 요청자가 호스트가 아닌 경우
	 */
	public void validateHostPermission(ScheduleEntity schedule, String userTsid) {
		if (!schedule.isHostedBy(userTsid)) {
			throw new BusinessException(ErrorCode.SCHEDULE_PERMISSION_DENIED);
		}
	}
}
