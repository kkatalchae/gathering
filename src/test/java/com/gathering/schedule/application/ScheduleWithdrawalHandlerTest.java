package com.gathering.schedule.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;

/**
 * ScheduleWithdrawalHandler 테스트
 */
@ExtendWith(MockitoExtension.class)
class ScheduleWithdrawalHandlerTest {

	private static final String USER_TSID = "01HQUSER000001";

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@Mock
	private ScheduleService scheduleService;

	@InjectMocks
	private ScheduleWithdrawalHandler handler;

	@Test
	@DisplayName("호스트인 시작 전 일정을 락과 함께 조회해 삭제한 뒤, 시작 전 일정 참여를 해제한다")
	void deletesHostedUpcomingThenLeavesUpcoming() {
		// given
		ScheduleEntity hosted = ScheduleEntity.builder()
			.tsid("01HQSCHEDULE01")
			.title("호스트 일정")
			.startAt(Instant.now().plusSeconds(3600))
			.locationName("한강공원")
			.createdBy(USER_TSID)
			.build();
		given(scheduleRepository.findAllUpcomingByHostForUpdate(eq(USER_TSID), any(Instant.class)))
			.willReturn(List.of(hosted));

		// when
		handler.cleanUp(USER_TSID);

		// then
		InOrder order = inOrder(scheduleService, scheduleParticipantRepository);
		order.verify(scheduleService).deleteSchedulesByTsids(List.of("01HQSCHEDULE01"));
		order.verify(scheduleParticipantRepository).deleteAllUpcomingByUserTsid(eq(USER_TSID), any(Instant.class));
	}

	@Test
	@DisplayName("호스트인 시작 전 일정이 없어도 참여 해제는 실행된다")
	void leavesUpcomingEvenWithoutHostedSchedules() {
		// given
		given(scheduleRepository.findAllUpcomingByHostForUpdate(eq(USER_TSID), any(Instant.class)))
			.willReturn(List.of());

		// when
		handler.cleanUp(USER_TSID);

		// then
		then(scheduleService).should().deleteSchedulesByTsids(List.of());
		then(scheduleParticipantRepository).should().deleteAllUpcomingByUserTsid(eq(USER_TSID), any(Instant.class));
	}
}
