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
import org.springframework.data.domain.Pageable;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleParticipantCount;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.schedule.domain.model.ScheduleTimeFilter;
import com.gathering.schedule.domain.policy.SchedulePolicy;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.schedule.presentation.dto.CreateScheduleRequest;
import com.gathering.schedule.presentation.dto.JoinScheduleResponse;
import com.gathering.schedule.presentation.dto.ScheduleDetailResponse;
import com.gathering.schedule.presentation.dto.ScheduleListRequest;
import com.gathering.schedule.presentation.dto.ScheduleListResponse;
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
	private GatheringParticipantRepository gatheringParticipantRepository;

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

		// 호스트가 첫 참여자로 등록된다
		ArgumentCaptor<ScheduleParticipantEntity> participantCaptor =
			ArgumentCaptor.forClass(ScheduleParticipantEntity.class);
		then(scheduleParticipantRepository).should().save(participantCaptor.capture());
		assertThat(participantCaptor.getValue().getUserTsid()).isEqualTo(HOST_TSID);
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
	@DisplayName("모임 TSID가 빈 문자열이면 모임을 선택하지 않은 것으로 보고 독립 일정을 생성한다")
	void createStandaloneScheduleWithBlankGatheringTsid() {
		// given: 폼에서 모임을 선택하지 않으면 빈 문자열이 전달된다
		CreateScheduleRequest request = createRequest("   ", null);
		given(scheduleRepository.save(any(ScheduleEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ScheduleResponse response = scheduleService.createSchedule(HOST_TSID, request);

		// then
		then(schedulePolicy).should(never()).validateGatheringParticipant(any(), any());
		ArgumentCaptor<ScheduleEntity> captor = ArgumentCaptor.forClass(ScheduleEntity.class);
		then(scheduleRepository).should().save(captor.capture());
		assertThat(captor.getValue().getGatheringTsid()).isNull();
		assertThat(response.getGatheringTsid()).isNull();
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
	@DisplayName("일정 목록 조회 시 size+1 조회로 hasNext를 판단하고 마지막 항목으로 다음 커서를 만든다")
	void getSchedulesWithNextPage() {
		// given: size=2 요청에 3건이 반환되면 다음 페이지가 있다
		ScheduleEntity first = listSchedule("01HQSCHEDULE01", START_AT);
		ScheduleEntity second = listSchedule("01HQSCHEDULE02", START_AT);
		ScheduleEntity third = listSchedule("01HQSCHEDULE03", START_AT.plusSeconds(3600));
		given(scheduleRepository.findUpcomingSchedules(isNull(), any(Instant.class), isNull(), isNull(),
			any(Pageable.class))).willReturn(List.of(first, second, third));
		given(scheduleParticipantRepository.countByScheduleTsidIn(List.of("01HQSCHEDULE01", "01HQSCHEDULE02")))
			.willReturn(List.of(new ScheduleParticipantCount("01HQSCHEDULE02", 3L)));

		ScheduleListRequest request = ScheduleListRequest.builder().size(2).build();

		// when
		ScheduleListResponse response = scheduleService.getSchedules(request);

		// then
		assertThat(response.getSchedules()).hasSize(2);
		assertThat(response.getHasNext()).isTrue();
		assertThat(response.getNextCursor()).isEqualTo(START_AT + "_01HQSCHEDULE02");
		assertThat(response.getSchedules().get(0).getParticipantCount()).isZero();
		assertThat(response.getSchedules().get(1).getParticipantCount()).isEqualTo(3L);

		// 참여 인원 집계는 일정 수와 무관하게 한 번만 조회한다
		then(scheduleParticipantRepository).should(times(1)).countByScheduleTsidIn(any());
		then(scheduleParticipantRepository).should(never()).countByScheduleTsid(any());
	}

	@Test
	@DisplayName("커서와 모임 필터를 주면 해석한 값으로 리포지토리를 호출한다")
	void getSchedulesWithCursorAndGatheringFilter() {
		// given
		String cursor = START_AT + "_01HQSCHEDULE01";
		ScheduleEntity next = listSchedule("01HQSCHEDULE02", START_AT.plusSeconds(3600));
		given(scheduleRepository.findUpcomingSchedules(eq(GATHERING_TSID), any(Instant.class), eq(START_AT),
			eq("01HQSCHEDULE01"), any(Pageable.class))).willReturn(List.of(next));
		given(scheduleParticipantRepository.countByScheduleTsidIn(List.of("01HQSCHEDULE02"))).willReturn(List.of());

		ScheduleListRequest request = ScheduleListRequest.builder()
			.gatheringTsid(GATHERING_TSID)
			.cursor(cursor)
			.size(20)
			.build();

		// when
		ScheduleListResponse response = scheduleService.getSchedules(request);

		// then
		assertThat(response.getSchedules()).extracting("tsid").containsExactly("01HQSCHEDULE02");
		assertThat(response.getHasNext()).isFalse();
		assertThat(response.getNextCursor()).isNull();
	}

	@Test
	@DisplayName("PAST 필터를 주면 지난 일정 쿼리를 사용한다")
	void getSchedulesWithPastFilter() {
		// given
		given(scheduleRepository.findPastSchedules(isNull(), any(Instant.class), isNull(), isNull(),
			any(Pageable.class))).willReturn(List.of());

		ScheduleListRequest request = ScheduleListRequest.builder().timeFilter(ScheduleTimeFilter.PAST).build();

		// when
		ScheduleListResponse response = scheduleService.getSchedules(request);

		// then
		assertThat(response.getSchedules()).isEmpty();
		then(scheduleRepository).should(never()).findUpcomingSchedules(any(), any(), any(), any(), any());
		// 조회 결과가 없으면 참여 인원 집계 쿼리도 실행하지 않는다
		then(scheduleParticipantRepository).should(never()).countByScheduleTsidIn(any());
	}

	@Test
	@DisplayName("형식이 잘못된 커서로 조회하면 예외가 발생한다")
	void getSchedulesWithInvalidCursor() {
		// given
		ScheduleListRequest request = ScheduleListRequest.builder().cursor("broken-cursor").build();

		// when & then
		assertThatThrownBy(() -> scheduleService.getSchedules(request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURSOR);
		then(scheduleRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("귀속 일정 상세 조회 시 참여자마다 호스트 여부와 모임 소속 여부가 함께 반환된다")
	void getScheduleDetailWithGatheringMemberFlags() {
		// given: 호스트(모임 멤버) + 모임 멤버 1명 + 게스트 1명
		String memberTsid = "01HQUSERMEMBER";
		String guestTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.gatheringTsid(GATHERING_TSID)
			.gathering(GatheringEntity.builder()
				.tsid(GATHERING_TSID).name("한강 러닝크루").build())
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.maxParticipants(4)
			.createdBy(HOST_TSID)
			.creator(user(HOST_TSID, "호스트"))
			.build();
		given(scheduleRepository.findByTsidWithCreatorAndGathering(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.findAllByScheduleTsidWithUser(SCHEDULE_TSID)).willReturn(List.of(
			participant(HOST_TSID, "호스트"),
			participant(memberTsid, "멤버"),
			participant(guestTsid, "게스트")));
		given(gatheringParticipantRepository.findUserTsidsByGatheringTsidAndUserTsidIn(
			GATHERING_TSID, List.of(HOST_TSID, memberTsid, guestTsid)))
			.willReturn(List.of(HOST_TSID, memberTsid));

		// when
		ScheduleDetailResponse response = scheduleService.getScheduleDetail(SCHEDULE_TSID);

		// then
		assertThat(response.getGatheringName()).isEqualTo("한강 러닝크루");
		assertThat(response.getParticipants()).extracting("userTsid", "host", "gatheringMember")
			.containsExactly(
				tuple(HOST_TSID, true, true),
				tuple(memberTsid, false, true),
				tuple(guestTsid, false, false));
		// 소속 판정은 참여자 수와 무관하게 한 번만 조회한다
		then(gatheringParticipantRepository).should(times(1)).findUserTsidsByGatheringTsidAndUserTsidIn(any(), any());
	}

	@Test
	@DisplayName("독립 일정 상세 조회 시 모임 소속 조회 없이 전원 게스트로 반환된다")
	void getScheduleDetailForStandaloneSchedule() {
		// given
		ScheduleEntity schedule = ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.createdBy(HOST_TSID)
			.creator(user(HOST_TSID, "호스트"))
			.build();
		given(scheduleRepository.findByTsidWithCreatorAndGathering(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.findAllByScheduleTsidWithUser(SCHEDULE_TSID))
			.willReturn(List.of(participant(HOST_TSID, "호스트"), participant("01HQUSERGUEST1", "게스트")));

		// when
		ScheduleDetailResponse response = scheduleService.getScheduleDetail(SCHEDULE_TSID);

		// then
		assertThat(response.getGatheringName()).isNull();
		assertThat(response.getHostNickname()).isEqualTo("호스트");
		assertThat(response.getParticipants()).extracting("gatheringMember").containsOnly(false);
		then(gatheringParticipantRepository).shouldHaveNoInteractions();
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
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
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
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
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
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
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
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));

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
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scheduleService.deleteSchedule(SCHEDULE_TSID, HOST_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		then(scheduleRepository).should(never()).deleteById(any());
	}

	// ==================== 일정 참여/취소 테스트 ====================

	@Test
	@DisplayName("정원이 남은 일정에 참여하면 참여자가 생성된다")
	void joinScheduleSuccess() {
		// given: 정원 4명 중 3명 참여 중
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = upcomingSchedule(4);
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(SCHEDULE_TSID, userTsid)).willReturn(false);
		given(scheduleParticipantRepository.countByScheduleTsid(SCHEDULE_TSID)).willReturn(3L);
		given(scheduleParticipantRepository.save(any(ScheduleParticipantEntity.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		JoinScheduleResponse response = scheduleService.joinSchedule(SCHEDULE_TSID, userTsid);

		// then
		assertThat(response.getScheduleTsid()).isEqualTo(SCHEDULE_TSID);
		assertThat(response.getUserTsid()).isEqualTo(userTsid);
	}

	@Test
	@DisplayName("정원이 없는 일정에는 인원과 무관하게 참여할 수 있다")
	void joinScheduleWithoutCapacity() {
		// given
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = upcomingSchedule(null);
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(SCHEDULE_TSID, userTsid)).willReturn(false);
		given(scheduleParticipantRepository.countByScheduleTsid(SCHEDULE_TSID)).willReturn(999L);
		given(scheduleParticipantRepository.save(any(ScheduleParticipantEntity.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when & then
		assertThatCode(() -> scheduleService.joinSchedule(SCHEDULE_TSID, userTsid)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("정원이 가득 찬 일정에 참여하면 예외가 발생한다")
	void joinScheduleWhenFull() {
		// given: 정원 4명 중 4명 참여 중
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = upcomingSchedule(4);
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(SCHEDULE_TSID, userTsid)).willReturn(false);
		given(scheduleParticipantRepository.countByScheduleTsid(SCHEDULE_TSID)).willReturn(4L);

		// when & then
		assertThatThrownBy(() -> scheduleService.joinSchedule(SCHEDULE_TSID, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_CAPACITY_EXCEEDED);
		then(scheduleParticipantRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("이미 참여중인 일정에 다시 참여하면 예외가 발생한다")
	void joinScheduleAlreadyJoined() {
		// given
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = upcomingSchedule(4);
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(SCHEDULE_TSID, userTsid)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> scheduleService.joinSchedule(SCHEDULE_TSID, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_JOINED_SCHEDULE);
		then(scheduleParticipantRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("이미 시작된 일정에 참여하면 예외가 발생한다")
	void joinScheduleAlreadyStarted() {
		// given
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.title("지난 일정")
			.startAt(Instant.now().minusSeconds(60))
			.locationName("한강공원 2주차장")
			.createdBy(HOST_TSID)
			.build();
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> scheduleService.joinSchedule(SCHEDULE_TSID, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ALREADY_STARTED);
		then(scheduleParticipantRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("존재하지 않는 일정에 참여하면 예외가 발생한다")
	void joinScheduleNotFound() {
		// given
		given(scheduleRepository.findByTsidForUpdate(SCHEDULE_TSID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scheduleService.joinSchedule(SCHEDULE_TSID, "01HQUSERGUEST1"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
	}

	@Test
	@DisplayName("참여중인 일정에서 빠지면 참여자가 삭제된다")
	void leaveScheduleSuccess() {
		// given
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = upcomingSchedule(4);
		ScheduleParticipantEntity participant = participant(userTsid, "게스트");
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.findByScheduleTsidAndUserTsid(SCHEDULE_TSID, userTsid))
			.willReturn(Optional.of(participant));

		// when
		scheduleService.leaveSchedule(SCHEDULE_TSID, userTsid);

		// then
		then(scheduleParticipantRepository).should().delete(participant);
	}

	@Test
	@DisplayName("호스트는 일정에서 빠질 수 없다")
	void leaveScheduleAsHost() {
		// given
		ScheduleEntity schedule = upcomingSchedule(4);
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> scheduleService.leaveSchedule(SCHEDULE_TSID, HOST_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOST_CANNOT_LEAVE_SCHEDULE);
		then(scheduleParticipantRepository).should(never()).delete(any());
	}

	@Test
	@DisplayName("참여하지 않은 일정에서 빠지려 하면 예외가 발생한다")
	void leaveScheduleNotParticipant() {
		// given
		String userTsid = "01HQUSERGUEST1";
		ScheduleEntity schedule = upcomingSchedule(4);
		given(scheduleRepository.findById(SCHEDULE_TSID)).willReturn(Optional.of(schedule));
		given(scheduleParticipantRepository.findByScheduleTsidAndUserTsid(SCHEDULE_TSID, userTsid))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scheduleService.leaveSchedule(SCHEDULE_TSID, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND);
	}

	@Test
	@DisplayName("모임 삭제 시 귀속 일정의 참여자를 한 번의 쿼리로 삭제한 뒤 일정을 삭제한다")
	void deleteSchedulesByGatheringTsid() {
		// given
		List<String> scheduleTsids = List.of(SCHEDULE_TSID, "01HQSCHEDULE02");
		given(scheduleRepository.findAllByGatheringTsidForUpdate(GATHERING_TSID)).willReturn(List.of(
			listSchedule(SCHEDULE_TSID, START_AT), listSchedule("01HQSCHEDULE02", START_AT)));

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
		given(scheduleRepository.findAllByGatheringTsidForUpdate(GATHERING_TSID)).willReturn(List.of());

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

	private ScheduleEntity listSchedule(String tsid, Instant startAt) {
		return ScheduleEntity.builder()
			.tsid(tsid)
			.title("9월 정기 모임")
			.startAt(startAt)
			.locationName("한강공원 2주차장")
			.createdBy(HOST_TSID)
			.creator(UsersEntity.builder().tsid(HOST_TSID).nickname("호스트").name("김병채").build())
			.build();
	}

	private UsersEntity user(String tsid, String nickname) {
		return UsersEntity.builder().tsid(tsid).nickname(nickname).name("이름").build();
	}

	private ScheduleParticipantEntity participant(String userTsid, String nickname) {
		return ScheduleParticipantEntity.builder()
			.tsid("01HQSP" + userTsid.substring(6))
			.scheduleTsid(SCHEDULE_TSID)
			.userTsid(userTsid)
			.joinedAt(START_AT)
			.user(user(userTsid, nickname))
			.build();
	}

	private ScheduleEntity upcomingSchedule(Integer maxParticipants) {
		return ScheduleEntity.builder()
			.tsid(SCHEDULE_TSID)
			.title("9월 정기 모임")
			.startAt(Instant.now().plusSeconds(86400))
			.locationName("한강공원 2주차장")
			.maxParticipants(maxParticipants)
			.createdBy(HOST_TSID)
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
