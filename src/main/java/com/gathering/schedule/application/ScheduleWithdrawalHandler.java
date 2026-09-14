package com.gathering.schedule.application;

import java.time.Instant;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.user.domain.policy.UserWithdrawalHandler;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴 시 일정 도메인 정리
 * - 호스트인 일정 중 아직 시작하지 않은 것은 참여자와 함께 삭제한다 (호스트 없이 진행될 수 없다)
 * - 참여 중인 일정 중 아직 시작하지 않은 것은 참여를 해제한다
 * - 지난 일정은 호스트든 참여자든 모임의 기록이므로 손대지 않는다 (사용자 row 가 익명화되어 남는다)
 * 모임 정리보다 먼저 실행해 락 순서(모임 → 일정)를 어기지 않는다 — 여기서는 일정 row 락만 잡는다
 */
@Component
@Order(ScheduleWithdrawalHandler.ORDER)
@RequiredArgsConstructor
public class ScheduleWithdrawalHandler implements UserWithdrawalHandler {

	public static final int ORDER = 100;

	private final ScheduleRepository scheduleRepository;
	private final ScheduleParticipantRepository scheduleParticipantRepository;
	private final ScheduleService scheduleService;

	@Override
	public void cleanUp(String userTsid) {
		Instant now = Instant.now();

		// 호스트인 시작 전 일정: 락을 잡고 참여자 → 일정 순으로 삭제
		List<String> hostedUpcomingTsids = scheduleRepository.findAllUpcomingByHostForUpdate(userTsid, now).stream()
			.map(ScheduleEntity::getTsid)
			.toList();
		scheduleService.deleteSchedulesByTsids(hostedUpcomingTsids);

		// 참여 중인 시작 전 일정: 참여만 해제 (위에서 삭제된 일정의 참여는 이미 사라졌다)
		scheduleParticipantRepository.deleteAllUpcomingByUserTsid(userTsid, now);
	}
}
