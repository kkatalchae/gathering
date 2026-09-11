package com.gathering.schedule.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.policy.SchedulePolicy;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.schedule.presentation.dto.CreateScheduleRequest;
import com.gathering.schedule.presentation.dto.ScheduleDetailResponse;
import com.gathering.schedule.presentation.dto.ScheduleResponse;
import com.gathering.schedule.presentation.dto.UpdateScheduleRequest;
import com.gathering.user.domain.model.UsersEntity;

/**
 * ScheduleService 테스트
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

	private static final String GATHERING_TSID = "01HQGATHERING1";
	private static final String SCHEDULE_TSID = "01HQSCHEDULE01";
	private static final String HOST_TSID = "01HQUSERHOST01";
	private static final Instant START_AT = Instant.parse("2026-09-20T10:00:00Z");

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@Mock
	private SchedulePolicy schedulePolicy;

	@InjectMocks
	private ScheduleService scheduleService;

	@Test
	@DisplayName("모임에 귀속된 일정을 생성하면 개설자가 호스트로 저장된다")
	void createGatheringScheduleSuccess() {
		// given
		CreateScheduleRequest request = createRequest(GATHERING_TSID, 4);
		given(scheduleRepository.save(any(ScheduleEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ScheduleResponse response = scheduleService.createSchedule(HOST_TSID, request);

		// then
		then(schedulePolicy).should().validateGatheringParticipant(GATHERING_TSID, HOST_TSID);

		ArgumentCaptor<ScheduleEntity> captor = ArgumentCaptor.forClass(ScheduleEntity.class);
		then(scheduleRepository).should().save(captor.capture());
		ScheduleEntity saved = captor.getValue();
		assertThat(saved.getGatheringTsid()).isEqualTo(GATHERING_TSID);
		assertThat(saved.getCreatedBy()).isEqualTo(HOST_TSID);
		assertThat(saved.getLocationName()).isEqualTo("한강공원 2주차장");
		assertThat(response.getHostTsid()).isEqualTo(HOST_TSID);
		assertThat(response.getMaxParticipants()).isEqualTo(4);
	}

	@Test
	@DisplayName("모임을 지정하지 않으면 모임 참여 검증 없이 독립 일정이 생성된다")
	void createStandaloneScheduleSuccess() {
		// given
		CreateScheduleRequest request = createRequest(null, null);
		given(scheduleRepository.save(any(ScheduleEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ScheduleResponse response = scheduleService.createSchedule(HOST_TSID, request);

		// then
		then(schedulePolicy).should(never()).validateGatheringParticipant(any(), any());
		assertThat(response.getGatheringTsid()).isNull();
		assertThat(response.getMaxParticipants()).isNull();
	}

	@Test
	@DisplayName("모임 참여자가 아니면 일정이 저장되지 않는다")
	void createGatheringScheduleWithoutParticipation() {
		// given
		CreateScheduleRequest request = createRequest(GATHERING_TSID, null);
		willThrow(new BusinessException(ErrorCode.NOT_GATHERING_PARTICIPANT))
			.given(schedulePolicy).validateGatheringParticipant(GATHERING_TSID, HOST_TSID);

		// when & then
		assertThatThrownBy(() -> scheduleService.createSchedule(HOST_TSID, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_GATHERING_PARTICIPANT);
		then(scheduleRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("장소 없이 일정을 생성하면 예외가 발생한다")
	void createScheduleWithoutLocation() {
		// given
		CreateScheduleRequest request = CreateScheduleRequest.builder()
			.title("9월 정기 모임")
			.startAt(START_AT)
			.build();

		// when & then
		assertThatThrownBy(() -> scheduleService.createSchedule(HOST_TSID, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_LOCATION_NAME_REQUIRED);
		then(scheduleRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("종료 시각이 시작 시각보다 앞서면 일정이 저장되지 않는다")
	void createScheduleWithInvalidPeriod() {
		// given
		CreateScheduleRequest request = CreateScheduleRequest.builder()
			.title("9월 정기 모임")
			.startAt(START_AT)
			.endAt(START_AT.minusSeconds(60))
			.locationName("한강공원 2주차장")
			.build();

		// when & then
		assertThatThrownBy(() -> scheduleService.createSchedule(HOST_TSID, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_END_AT_BEFORE_START_AT);
		then(scheduleRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("일정 상세 조회 시 호스트 정보와 현재 참여 인원이 함께 반환된다")
	void getScheduleDetailSuccess() {
		// given
		UsersEntity host = UsersEntity.builder()
			.tsid(HOST_TSID)
			.nickname("호스트")
			.name("김병채")
			.profileImageUrl("https://example.com/host.jpg")
			.build();
		ScheduleEntity schedule = ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.maxParticipants(4)
			.createdBy(HOST_TSID)
			.creator(host)
			.build();
		given(scheduleRepository.findByTsidWithCreatorAndGathering(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.countByScheduleTsid(SCHEDULE_TSID)).willReturn(3L);

		// when
		ScheduleDetailResponse response = scheduleService.getScheduleDetail(SCHEDULE_TSID);

		// then
		assertThat(response.getHostNickname()).isEqualTo("호스트");
		assertThat(response.getParticipantCount()).isEqualTo(3L);
		assertThat(response.getGatheringName()).isNull();
	}

	@Test
	@DisplayName("존재하지 않는 일정을 조회하면 예외가 발생한다")
	void getScheduleDetailNotFound() {
		// given
		given(scheduleRepository.findByTsidWithCreatorAndGathering(SCHEDULE_TSID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scheduleService.getScheduleDetail(SCHEDULE_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
	}

	@Test
	@DisplayName("일정 수정에 성공하면 변경된 값이 엔티티에 반영된다")
	void updateScheduleSuccess() {
		// given
		ScheduleEntity schedule = standaloneSchedule();
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.countByScheduleTsid(SCHEDULE_TSID)).willReturn(2L);

		UpdateScheduleRequest request = UpdateScheduleRequest.builder()
			.title("9월 정기 모임 (장소 변경)")
			.description("비가 와서 실내로 변경합니다")
			.startAt(START_AT)
			.endAt(START_AT.plusSeconds(7200))
			.locationName("합정역 3번 출구")
			.locationAddress("서울특별시 마포구 양화로 45")
			.maxParticipants(6)
			.build();

		// when
		ScheduleResponse response = scheduleService.updateSchedule(SCHEDULE_TSID, HOST_TSID, request);

		// then
		then(schedulePolicy).should().validateHostPermission(schedule, HOST_TSID);
		assertThat(schedule.getTitle()).isEqualTo("9월 정기 모임 (장소 변경)");
		assertThat(schedule.getLocationName()).isEqualTo("합정역 3번 출구");
		assertThat(response.getMaxParticipants()).isEqualTo(6);
	}

	@Test
	@DisplayName("현재 참여 인원보다 적은 정원으로 수정하면 예외가 발생한다")
	void updateScheduleWithCapacityBelowParticipantCount() {
		// given
		ScheduleEntity schedule = standaloneSchedule();
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.countByScheduleTsid(SCHEDULE_TSID)).willReturn(5L);

		UpdateScheduleRequest request = UpdateScheduleRequest.builder()
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.maxParticipants(4)
			.build();

		// when & then
		assertThatThrownBy(() -> scheduleService.updateSchedule(SCHEDULE_TSID, HOST_TSID, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_CAPACITY_BELOW_PARTICIPANT_COUNT);
		assertThat(schedule.getMaxParticipants()).isEqualTo(4);
		assertThat(schedule.getTitle()).isEqualTo("9월 정기 모임");
	}

	@Test
	@DisplayName("관리 권한이 없으면 일정이 수정되지 않는다")
	void updateScheduleWithoutPermission() {
		// given
		ScheduleEntity schedule = standaloneSchedule();
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		willThrow(new BusinessException(ErrorCode.SCHEDULE_PERMISSION_DENIED))
			.given(schedulePolicy).validateHostPermission(schedule, "01HQUSEROTHER1");

		UpdateScheduleRequest request = UpdateScheduleRequest.builder()
			.title("몰래 바꾼 제목")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.build();

		// when & then
		assertThatThrownBy(() -> scheduleService.updateSchedule(SCHEDULE_TSID, "01HQUSEROTHER1", request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_PERMISSION_DENIED);
		assertThat(schedule.getTitle()).isEqualTo("9월 정기 모임");
	}

	@Test
	@DisplayName("일정을 삭제하면 참여자를 먼저 삭제한 뒤 일정이 삭제된다")
	void deleteScheduleSuccess() {
		// given
		ScheduleEntity schedule = standaloneSchedule();
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));

		// when
		scheduleService.deleteSchedule(SCHEDULE_TSID, HOST_TSID);

		// then
		then(schedulePolicy).should().validateHostPermission(schedule, HOST_TSID);
		then(scheduleParticipantRepository).should().deleteAllByScheduleTsid(SCHEDULE_TSID);
		then(scheduleRepository).should().deleteById(SCHEDULE_TSID);
	}

	@Test
	@DisplayName("존재하지 않는 일정을 삭제하면 예외가 발생한다")
	void deleteScheduleNotFound() {
		// given
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scheduleService.deleteSchedule(SCHEDULE_TSID, HOST_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		then(scheduleRepository).should(never()).deleteById(any());
	}

	@Test
	@DisplayName("모임 삭제 시 귀속 일정의 참여자를 한 번의 쿼리로 삭제한 뒤 일정을 삭제한다")
	void deleteSchedulesByGatheringTsid() {
		// given
		List<String> scheduleTsids = List.of(SCHEDULE_TSID, "01HQSCHEDULE02");
		given(scheduleRepository.findTsidsByGatheringTsid(GATHERING_TSID)).willReturn(scheduleTsids);

		// when
		scheduleService.deleteSchedulesByGatheringTsid(GATHERING_TSID);

		// then
		then(scheduleParticipantRepository).should().deleteAllByScheduleTsidIn(scheduleTsids);
		then(scheduleRepository).should().deleteAllByGatheringTsid(GATHERING_TSID);
	}

	@Test
	@DisplayName("모임에 귀속된 일정이 없으면 삭제 쿼리를 실행하지 않는다")
	void deleteSchedulesByGatheringTsidWithoutSchedules() {
		// given
		given(scheduleRepository.findTsidsByGatheringTsid(GATHERING_TSID)).willReturn(List.of());

		// when
		scheduleService.deleteSchedulesByGatheringTsid(GATHERING_TSID);

		// then
		then(scheduleParticipantRepository).should(never()).deleteAllByScheduleTsidIn(any());
		then(scheduleRepository).should(never()).deleteAllByGatheringTsid(any());
	}

	private CreateScheduleRequest createRequest(String gatheringTsid, Integer maxParticipants) {
		return CreateScheduleRequest.builder()
			.gatheringTsid(gatheringTsid)
			.title("9월 정기 모임")
			.description("한강에서 러닝 후 저녁 식사")
			.startAt(START_AT)
			.endAt(START_AT.plusSeconds(7200))
			.locationName("한강공원 2주차장")
			.locationAddress("서울특별시 영등포구 여의동로 330")
			.maxParticipants(maxParticipants)
			.build();
	}

	private ScheduleEntity standaloneSchedule() {
		return ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.maxParticipants(4)
			.createdBy(HOST_TSID)
			.build();
	}
}
