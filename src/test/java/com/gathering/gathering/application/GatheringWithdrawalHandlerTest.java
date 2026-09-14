package com.gathering.gathering.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;

/**
 * GatheringWithdrawalHandler 테스트
 */
@ExtendWith(MockitoExtension.class)
class GatheringWithdrawalHandlerTest {

	private static final String USER_TSID = "01HQUSER000001";

	@Mock
	private GatheringParticipantRepository participantRepository;

	@InjectMocks
	private GatheringWithdrawalHandler handler;

	@Test
	@DisplayName("오너인 모임이 있으면 탈퇴를 거부한다")
	void rejectsGatheringOwner() {
		// given
		given(participantRepository.existsByUserTsidAndRole(USER_TSID, ParticipantRole.OWNER)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> handler.validateWithdrawable(USER_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_WITHDRAW_AS_GATHERING_OWNER);
	}

	@Test
	@DisplayName("ADMIN 이나 MEMBER 로만 참여 중이면 탈퇴할 수 있다")
	void allowsNonOwner() {
		// given
		given(participantRepository.existsByUserTsidAndRole(USER_TSID, ParticipantRole.OWNER)).willReturn(false);

		// when & then
		assertThatCode(() -> handler.validateWithdrawable(USER_TSID)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("정리 단계에서 모든 모임 참여를 한 번의 쿼리로 해제한다")
	void cleanUpRemovesAllParticipations() {
		// when
		handler.cleanUp(USER_TSID);

		// then
		then(participantRepository).should().deleteAllByUserTsid(USER_TSID);
	}
}
