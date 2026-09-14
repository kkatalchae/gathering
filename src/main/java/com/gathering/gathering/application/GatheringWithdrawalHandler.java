package com.gathering.gathering.application;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.user.domain.policy.UserWithdrawalHandler;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴 시 모임 도메인 정리
 * - OWNER 인 모임이 하나라도 있으면 탈퇴를 거부한다: 다른 멤버들의 모임을 본인 모르게 없애거나 넘기지 않는다
 *   ("오너는 모임을 나갈 수 없다" 규칙과 같은 원칙). 양도하거나 삭제한 뒤 다시 탈퇴해야 한다
 * - 통과하면 모든 모임 참여를 해제한다
 */
@Component
@Order(GatheringWithdrawalHandler.ORDER)
@RequiredArgsConstructor
public class GatheringWithdrawalHandler implements UserWithdrawalHandler {

	public static final int ORDER = 200;

	private final GatheringParticipantRepository participantRepository;

	@Override
	public void validateWithdrawable(String userTsid) {
		if (participantRepository.existsByUserTsidAndRole(userTsid, ParticipantRole.OWNER)) {
			throw new BusinessException(ErrorCode.CANNOT_WITHDRAW_AS_GATHERING_OWNER);
		}
	}

	@Override
	public void cleanUp(String userTsid) {
		participantRepository.deleteAllByUserTsid(userTsid);
	}
}
