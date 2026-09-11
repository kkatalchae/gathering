package com.gathering.schedule.domain.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.schedule.domain.model.ScheduleEntity;

/**
 * SchedulePolicy 테스트
 */
@ExtendWith(MockitoExtension.class)
class SchedulePolicyTest {

	private static final String GATHERING_TSID = "01HQGATHERING1";
	private static final String SCHEDULE_TSID = "01HQSCHEDULE01";
	private static final String HOST_TSID = "01HQUSERHOST01";
	private static final String OTHER_USER_TSID = "01HQUSEROTHER1";

	@Mock
	private GatheringRepository gatheringRepository;

	@Mock
	private GatheringParticipantRepository gatheringParticipantRepository;

	@InjectMocks
	private SchedulePolicy schedulePolicy;

	@Test
	@DisplayName("모임 참여자는 해당 모임의 일정을 개설할 수 있다")
	void validateGatheringParticipantSuccess() {
		// given
		given(gatheringRepository.existsById(GATHERING_TSID)).willReturn(true);
		given(gatheringParticipantRepository.existsByGatheringTsidAndUserTsid(GATHERING_TSID, HOST_TSID))
			.willReturn(true);

		// when & then
		assertThatCode(() -> schedulePolicy.validateGatheringParticipant(GATHERING_TSID, HOST_TSID))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("존재하지 않는 모임의 일정을 개설하면 예외가 발생한다")
	void validateGatheringParticipantWithUnknownGathering() {
		// given
		given(gatheringRepository.existsById(GATHERING_TSID)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> schedulePolicy.validateGatheringParticipant(GATHERING_TSID, HOST_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NOT_FOUND);
	}

	@Test
	@DisplayName("모임에 참여하지 않은 사용자가 해당 모임의 일정을 개설하면 예외가 발생한다")
	void validateGatheringParticipantWithNonParticipant() {
		// given
		given(gatheringRepository.existsById(GATHERING_TSID)).willReturn(true);
		given(gatheringParticipantRepository.existsByGatheringTsidAndUserTsid(GATHERING_TSID, OTHER_USER_TSID))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> schedulePolicy.validateGatheringParticipant(GATHERING_TSID, OTHER_USER_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GATHERING_PARTICIPANT);
	}

	@Test
	@DisplayName("호스트는 자신이 개설한 일정을 관리할 수 있다")
	void validateHostPermissionForHost() {
		// given
		ScheduleEntity schedule = schedule(null);

		// when & then
		assertThatCode(() -> schedulePolicy.validateHostPermission(schedule, HOST_TSID))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("호스트가 아닌 사용자는 독립 일정을 관리할 수 없다")
	void validateHostPermissionForNonHostOfStandaloneSchedule() {
		// given
		ScheduleEntity schedule = schedule(null);

		// when & then
		assertThatThrownBy(() -> schedulePolicy.validateHostPermission(schedule, OTHER_USER_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_PERMISSION_DENIED);
	}

	@Test
	@DisplayName("귀속 모임의 OWNER/ADMIN이라도 호스트가 아니면 일정을 관리할 수 없다")
	void validateHostPermissionForGatheringOwner() {
		// given: 요청자가 모임 OWNER 인지는 확인조차 하지 않아야 한다
		ScheduleEntity schedule = schedule(GATHERING_TSID);

		// when & then
		assertThatThrownBy(() -> schedulePolicy.validateHostPermission(schedule, OTHER_USER_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_PERMISSION_DENIED);
		then(gatheringParticipantRepository).shouldHaveNoInteractions();
	}

	private ScheduleEntity schedule(String gatheringTsid) {
		return ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.gatheringTsid(gatheringTsid)
			.title("9월 정기 모임")
			.startAt(Instant.parse("2026-09-20T10:00:00Z"))
			.locationName("한강공원 2주차장")
			.createdBy(HOST_TSID)
			.build();
	}
}
