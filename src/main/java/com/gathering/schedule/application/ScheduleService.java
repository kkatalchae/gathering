package com.gathering.schedule.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.schedule.domain.model.ScheduleCapacity;
import com.gathering.schedule.domain.model.ScheduleCursor;
import com.gathering.schedule.domain.model.ScheduleDescription;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleLocation;
import com.gathering.schedule.domain.model.ScheduleParticipantCount;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.schedule.domain.model.SchedulePeriod;
import com.gathering.schedule.domain.model.ScheduleTitle;
import com.gathering.schedule.domain.policy.SchedulePolicy;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.schedule.presentation.dto.CreateScheduleRequest;
import com.gathering.schedule.presentation.dto.JoinScheduleResponse;
import com.gathering.schedule.presentation.dto.ScheduleDetailResponse;
import com.gathering.schedule.presentation.dto.ScheduleListItemResponse;
import com.gathering.schedule.presentation.dto.ScheduleListRequest;
import com.gathering.schedule.presentation.dto.ScheduleListResponse;
import com.gathering.schedule.presentation.dto.ScheduleParticipantSummary;
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
	private final GatheringParticipantRepository gatheringParticipantRepository;
	private final SchedulePolicy schedulePolicy;

	/**
	 * 일정 생성
	 * gatheringTsid가 있으면 모임 귀속 일정(모임 참여자만 개설 가능), 없으면 일회성 독립 일정
	 * 개설한 호스트는 자동으로 첫 참여자가 된다
	 *
	 * @param hostTsid 일정을 개설하는 사용자 TSID
	 * @param request 일정 생성 요청 DTO
	 * @return 생성된 일정 정보
	 */
	@Transactional
	public ScheduleResponse createSchedule(String hostTsid, CreateScheduleRequest request) {
		String gatheringTsid = normalizeGatheringTsid(request.getGatheringTsid());
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
		ScheduleEntity savedSchedule = scheduleRepository.save(schedule);

		// 호스트를 첫 참여자로 등록
		scheduleParticipantRepository.save(ScheduleParticipantEntity.builder()
			.scheduleTsid(savedSchedule.getTsid())
			.userTsid(hostTsid)
			.build());

		return ScheduleResponse.from(savedSchedule);
	}

	/**
	 * 일정 목록 조회
	 * 모임으로 필터링할 수 있으며 커서 기반 페이지네이션을 지원한다
	 * UPCOMING은 아직 시작하지 않은 일정을 가까운 순, PAST는 이미 시작한 일정을 최근 순으로 반환한다
	 *
	 * @param request 필터링 및 페이지네이션 요청
	 * @return 일정 목록 및 다음 커서 정보
	 */
	@Transactional(readOnly = true)
	public ScheduleListResponse getSchedules(ScheduleListRequest request) {
		Instant now = Instant.now();
		ScheduleCursor cursor = ScheduleCursor.decode(request.getCursor());
		Instant cursorStartAt = cursor == null ? null : cursor.getStartAt();
		String cursorTsid = cursor == null ? null : cursor.getTsid();

		// size+1로 조회하여 hasNext 판단
		Pageable pageable = PageRequest.of(0, request.getSize() + 1);
		List<ScheduleEntity> schedules = switch (request.getTimeFilter()) {
			case UPCOMING -> scheduleRepository.findUpcomingSchedules(
				request.getGatheringTsid(), now, cursorStartAt, cursorTsid, pageable);
			case PAST -> scheduleRepository.findPastSchedules(
				request.getGatheringTsid(), now, cursorStartAt, cursorTsid, pageable);
		};

		boolean hasNext = schedules.size() > request.getSize();
		if (hasNext) {
			schedules = schedules.subList(0, request.getSize());
		}

		Map<String, Long> participantCounts = countParticipants(schedules);
		List<ScheduleListItemResponse> items = schedules.stream()
			.map(schedule -> ScheduleListItemResponse.from(
				schedule, participantCounts.getOrDefault(schedule.getTsid(), 0L)))
			.toList();

		String nextCursor = hasNext ? ScheduleCursor.from(schedules.getLast()).encode() : null;

		return ScheduleListResponse.of(items, nextCursor, hasNext);
	}

	/**
	 * 일정 상세 조회
	 * 참여자 목록에는 호스트 여부와 귀속 모임 소속 여부가 함께 담긴다
	 *
	 * @param scheduleTsid 일정 TSID
	 * @return 일정 상세 정보 (호스트 정보와 전체 참여자 목록 포함)
	 */
	@Transactional(readOnly = true)
	public ScheduleDetailResponse getScheduleDetail(String scheduleTsid) {
		ScheduleEntity schedule = scheduleRepository.findByTsidWithCreatorAndGathering(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		List<ScheduleParticipantEntity> participants =
			scheduleParticipantRepository.findAllByScheduleTsidWithUser(scheduleTsid);
		Set<String> gatheringMemberTsids = findGatheringMemberTsids(schedule, participants);

		List<ScheduleParticipantSummary> summaries = participants.stream()
			.map(participant -> ScheduleParticipantSummary.from(
				participant,
				schedule.isHostedBy(participant.getUserTsid()),
				gatheringMemberTsids.contains(participant.getUserTsid())))
			.toList();

		return ScheduleDetailResponse.from(schedule, summaries);
	}

	/**
	 * 일정 참여
	 * 정원 확인과 참여자 저장이 원자적으로 이루어지도록 일정 row 에 쓰기 락을 건다
	 * 락 안에서는 DB 작업만 하고 외부 I/O(알림 전송 등)는 하지 않는다 — docs/adr/0001 참고
	 *
	 * @param scheduleTsid 참여할 일정 TSID
	 * @param userTsid 참여할 사용자 TSID
	 * @return 생성된 참여자 정보
	 */
	@Transactional
	public JoinScheduleResponse joinSchedule(String scheduleTsid, String userTsid) {
		ScheduleEntity schedule = scheduleRepository.findByTsidForUpdate(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		// 이미 시작된 일정에는 참여 불가
		SchedulePeriod period = new SchedulePeriod(schedule.getStartAt(), schedule.getEndAt());
		if (period.isStartedBefore(Instant.now())) {
			throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_STARTED);
		}

		// 이미 참여 중인지 확인
		if (scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(scheduleTsid, userTsid)) {
			throw new BusinessException(ErrorCode.ALREADY_JOINED_SCHEDULE);
		}

		// 정원 확인 (null 이면 제한 없음)
		ScheduleCapacity capacity = new ScheduleCapacity(schedule.getMaxParticipants());
		long currentCount = scheduleParticipantRepository.countByScheduleTsid(scheduleTsid);
		if (capacity.isFull(currentCount)) {
			throw new BusinessException(ErrorCode.SCHEDULE_CAPACITY_EXCEEDED);
		}

		ScheduleParticipantEntity saved = scheduleParticipantRepository.save(ScheduleParticipantEntity.builder()
			.scheduleTsid(scheduleTsid)
			.userTsid(userTsid)
			.build());

		return JoinScheduleResponse.from(saved);
	}

	/**
	 * 일정 참여 취소
	 * 이미 시작된 일정도 취소할 수 있지만 호스트는 빠질 수 없다 (일정 삭제로 대신)
	 * 인원이 줄어드는 방향이라 정원 불변식을 깨지 못하고 PK 삭제라 갭 락도 잡지 않으므로 일정 row 락은 잡지 않는다
	 *
	 * @param scheduleTsid 취소할 일정 TSID
	 * @param userTsid 취소할 사용자 TSID
	 */
	@Transactional
	public void leaveSchedule(String scheduleTsid, String userTsid) {
		ScheduleEntity schedule = scheduleRepository.findById(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.isHostedBy(userTsid)) {
			throw new BusinessException(ErrorCode.HOST_CANNOT_LEAVE_SCHEDULE);
		}

		ScheduleParticipantEntity participant = scheduleParticipantRepository
			.findByScheduleTsidAndUserTsid(scheduleTsid, userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));

		scheduleParticipantRepository.delete(participant);
	}

	/**
	 * 일정 수정
	 * 호스트만 수정 가능하며 귀속 모임은 변경할 수 없다
	 * 정원 축소 검증이 진행 중인 참여와 직렬화되도록 일정 row 에 쓰기 락을 건다
	 *
	 * @param scheduleTsid 수정할 일정 TSID
	 * @param userTsid 요청자 TSID
	 * @param request 수정 요청 DTO
	 * @return 수정된 일정 정보
	 */
	@Transactional
	public ScheduleResponse updateSchedule(String scheduleTsid, String userTsid, UpdateScheduleRequest request) {
		ScheduleEntity schedule = scheduleRepository.findByTsidForUpdate(scheduleTsid)
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
	 * 참여자 범위 삭제(갭 락) 전에 일정 row 락을 먼저 잡아, 동시에 들어온 참여와 락 순서가 엇갈려 데드락이 나지 않게 한다
	 *
	 * @param scheduleTsid 삭제할 일정 TSID
	 * @param userTsid 요청자 TSID
	 */
	@Transactional
	public void deleteSchedule(String scheduleTsid, String userTsid) {
		ScheduleEntity schedule = scheduleRepository.findByTsidForUpdate(scheduleTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		schedulePolicy.validateHostPermission(schedule, userTsid);

		// 참여자 데이터 삭제 (FK 제약 조건으로 인해 먼저 삭제)
		scheduleParticipantRepository.deleteAllByScheduleTsid(scheduleTsid);

		scheduleRepository.deleteById(scheduleTsid);
	}

	/**
	 * 모임에 귀속된 일정 전체 삭제
	 * 모임 삭제 시 FK 제약 조건을 만족시키기 위해 호출된다
	 * 일정 row 들의 락을 먼저 잡아 진행 중인 일정 참여가 끝난 뒤에 참여자를 범위 삭제한다 (락 순서: 모임 → 일정 → 참여자)
	 *
	 * @param gatheringTsid 삭제할 모임 TSID
	 */
	@Transactional
	public void deleteSchedulesByGatheringTsid(String gatheringTsid) {
		List<String> scheduleTsids = scheduleRepository.findAllByGatheringTsidForUpdate(gatheringTsid).stream()
			.map(ScheduleEntity::getTsid)
			.toList();
		if (scheduleTsids.isEmpty()) {
			return;
		}

		// 일정별로 반복하지 않고 IN 조건 한 번으로 참여자를 모두 삭제
		scheduleParticipantRepository.deleteAllByScheduleTsidIn(scheduleTsids);
		scheduleRepository.deleteAllByGatheringTsid(gatheringTsid);
	}

	/**
	 * 참여자 중 귀속 모임의 참여자인 사용자 TSID 집합을 한 번의 쿼리로 구한다
	 * 독립 일정은 귀속 모임이 없으므로 조회 없이 빈 집합 (전원 게스트)
	 */
	private Set<String> findGatheringMemberTsids(ScheduleEntity schedule, List<ScheduleParticipantEntity> participants) {
		if (!schedule.isAttachedToGathering() || participants.isEmpty()) {
			return Set.of();
		}

		List<String> userTsids = participants.stream().map(ScheduleParticipantEntity::getUserTsid).toList();
		return Set.copyOf(gatheringParticipantRepository.findUserTsidsByGatheringTsidAndUserTsidIn(
			schedule.getGatheringTsid(), userTsids));
	}

	/**
	 * 모임 TSID 정규화
	 * 폼에서 모임을 선택하지 않으면 빈 문자열이 올 수 있으므로, 공백뿐인 값은 "귀속 모임 없음"(null)으로 다룬다
	 */
	private String normalizeGatheringTsid(String gatheringTsid) {
		return StringUtils.hasText(gatheringTsid) ? gatheringTsid : null;
	}

	/**
	 * 일정별 참여 인원을 한 번의 쿼리로 집계해 Map으로 반환
	 * 참여자가 없는 일정은 Map에 없으므로 호출 측에서 0으로 취급한다
	 */
	private Map<String, Long> countParticipants(List<ScheduleEntity> schedules) {
		if (schedules.isEmpty()) {
			return Map.of();
		}

		List<String> scheduleTsids = schedules.stream().map(ScheduleEntity::getTsid).toList();
		// GROUP BY 결과라 일정 TSID는 유일하므로 키 충돌은 발생하지 않는다
		return scheduleParticipantRepository.countByScheduleTsidIn(scheduleTsids).stream()
			.collect(Collectors.toMap(
				ScheduleParticipantCount::scheduleTsid,
				ScheduleParticipantCount::participantCount));
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
