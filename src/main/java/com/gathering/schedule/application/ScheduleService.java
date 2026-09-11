package com.gathering.schedule.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.schedule.domain.model.ScheduleCapacity;
import com.gathering.schedule.domain.model.ScheduleDescription;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleLocation;
import com.gathering.schedule.domain.model.SchedulePeriod;
import com.gathering.schedule.domain.model.ScheduleTitle;
import com.gathering.schedule.domain.policy.SchedulePolicy;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.schedule.presentation.dto.CreateScheduleRequest;
import com.gathering.schedule.presentation.dto.ScheduleDetailResponse;
import com.gathering.schedule.presentation.dto.ScheduleResponse;
import com.gathering.schedule.presentation.dto.ScheduleValuesRequest;
import com.gathering.schedule.presentation.dto.UpdateScheduleRequest;

import lombok.RequiredArgsConstructor;

/**
 * 일정 Application Service
 * 유스케이스 조립만 담당 (값 검증은 VO, 권한 검증은 Policy가 처리)
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleParticipantRepository scheduleParticipantRepository;
	private final SchedulePolicy schedulePolicy;

	/**
	 * 일정 생성
	 * gatheringTsid가 있으면 모임 귀속 일정(모임 참여자만 개설 가능), 없으면 일회성 독립 일정
	 *
	 * @param hostTsid 일정을 개설하는 사용자 TSID
	 * @param request 일정 생성 요청 DTO
	 * @return 생성된 일정 정보
	 */
	@Transactional
	public ScheduleResponse createSchedule(String hostTsid, CreateScheduleRequest request) {
		String gatheringTsid = request.getGatheringTsid();
		if (gatheringTsid != null) {
			schedulePolicy.validateGatheringParticipant(gatheringTsid, hostTsid);
		}

		validateScheduleValues(request);

		ScheduleEntity schedule = ScheduleEntity.builder()
			.gatheringTsid(gatheringTsid)
			.title(request.getTitle())
			.description(request.getDescription())
			.startAt(request.getStartAt())
			.endAt(request.getEndAt())
			.locationName(request.getLocationName())
			.locationAddress(request.getLocationAddress())
			.maxParticipants(request.getMaxParticipants())
			.createdBy(hostTsid)
			.build();

		return ScheduleResponse.from(scheduleRepository.save(schedule));
	}

	/**
	 * 일정 상세 조회
	 *
	 * @param scheduleTsid 일정 TSID
	 * @return 일정 상세 정보 (호스트 정보와 현재 참여 인원 포함)
	 */
	@Transactional(readOnly = true)
	public ScheduleDetailResponse getScheduleDetail(String scheduleTsid) {
		ScheduleEntity schedule = scheduleRepository.findByTsidWithCreatorAndGathering(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		long participantCount = scheduleParticipantRepository.countByScheduleTsid(scheduleTsid);

		return ScheduleDetailResponse.from(schedule, participantCount);
	}

	/**
	 * 일정 수정
	 * 호스트만 수정 가능하며 귀속 모임은 변경할 수 없다
	 *
	 * @param scheduleTsid 수정할 일정 TSID
	 * @param userTsid 요청자 TSID
	 * @param request 수정 요청 DTO
	 * @return 수정된 일정 정보
	 */
	@Transactional
	public ScheduleResponse updateSchedule(String scheduleTsid, String userTsid, UpdateScheduleRequest request) {
		ScheduleEntity schedule = scheduleRepository.findById(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		schedulePolicy.validateHostPermission(schedule, userTsid);

		validateScheduleValues(request);
		validateCapacityCoversCurrentParticipants(scheduleTsid, request.getMaxParticipants());

		schedule.update(
			request.getTitle(),
			request.getDescription(),
			request.getStartAt(),
			request.getEndAt(),
			request.getLocationName(),
			request.getLocationAddress(),
			request.getMaxParticipants()
		);

		// Dirty Checking으로 자동 저장
		return ScheduleResponse.from(schedule);
	}

	/**
	 * 일정 삭제
	 * 호스트만 삭제 가능하며 참여자 데이터가 함께 삭제된다
	 *
	 * @param scheduleTsid 삭제할 일정 TSID
	 * @param userTsid 요청자 TSID
	 */
	@Transactional
	public void deleteSchedule(String scheduleTsid, String userTsid) {
		ScheduleEntity schedule = scheduleRepository.findById(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		schedulePolicy.validateHostPermission(schedule, userTsid);

		// 참여자 데이터 삭제 (FK 제약 조건으로 인해 먼저 삭제)
		scheduleParticipantRepository.deleteAllByScheduleTsid(scheduleTsid);

		scheduleRepository.deleteById(scheduleTsid);
	}

	/**
	 * 모임에 귀속된 일정 전체 삭제
	 * 모임 삭제 시 FK 제약 조건을 만족시키기 위해 호출된다
	 *
	 * @param gatheringTsid 삭제할 모임 TSID
	 */
	@Transactional
	public void deleteSchedulesByGatheringTsid(String gatheringTsid) {
		List<String> scheduleTsids = scheduleRepository.findTsidsByGatheringTsid(gatheringTsid);
		if (scheduleTsids.isEmpty()) {
			return;
		}

		// 일정별로 반복하지 않고 IN 조건 한 번으로 참여자를 모두 삭제
		scheduleParticipantRepository.deleteAllByScheduleTsidIn(scheduleTsids);
		scheduleRepository.deleteAllByGatheringTsid(gatheringTsid);
	}

	private void validateScheduleValues(ScheduleValuesRequest request) {

		// Value Object 생성 (값 유효성 자동 검증)
		new ScheduleTitle(request.getTitle());
		new ScheduleDescription(request.getDescription());
		new SchedulePeriod(request.getStartAt(), request.getEndAt());
		new ScheduleLocation(request.getLocationName(), request.getLocationAddress());
		new ScheduleCapacity(request.getMaxParticipants());
	}

	/**
	 * 이미 참여한 인원보다 적은 정원으로는 변경할 수 없다
	 * 정원을 지정하지 않은(무제한) 경우에는 확인이 필요 없다
	 */
	private void validateCapacityCoversCurrentParticipants(String scheduleTsid, Integer maxParticipants) {
		if (maxParticipants == null) {
			return;
		}

		long participantCount = scheduleParticipantRepository.countByScheduleTsid(scheduleTsid);
		if (participantCount > maxParticipants) {
			throw new BusinessException(ErrorCode.SCHEDULE_CAPACITY_BELOW_PARTICIPANT_COUNT);
		}
	}
}
