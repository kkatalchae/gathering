package com.gathering.schedule.application;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * 일정 참여 동시성 테스트
 * 남은 자리보다 많은 사용자가 동시에 참여를 시도해도 정확히 남은 자리만큼만 성공해야 한다
 * mock 으로는 검증할 수 없는 락 동작이므로 실제 DB(H2) 위에서 스레드를 띄워 확인한다
 * 근거: docs/adr/0001-capacity-concurrency-control.md
 */
@SpringBootTest
class ScheduleJoinConcurrencyTest {

	private static final int MAX_PARTICIPANTS = 3;
	private static final int CONCURRENT_USERS = 10;

	@Autowired
	private ScheduleService scheduleService;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private UsersEntity host;
	private List<UsersEntity> guests;
	private ScheduleEntity schedule;

	@BeforeEach
	void setUp() {
		host = usersRepository.save(user("host"));
		guests = new ArrayList<>();
		for (int i = 0; i < CONCURRENT_USERS; i++) {
			guests.add(usersRepository.save(user("guest" + i)));
		}

		// 호스트가 이미 1자리를 차지하므로 남은 자리는 MAX_PARTICIPANTS - 1
		schedule = scheduleRepository.save(ScheduleEntity.builder()
			.title("선착순 일정")
			.startAt(Instant.now().plus(Duration.ofDays(1)))
			.locationName("한강공원 2주차장")
			.maxParticipants(MAX_PARTICIPANTS)
			.createdBy(host.getTsid())
			.build());
		scheduleParticipantRepository.save(
			ScheduleParticipantEntity.builder()
				.scheduleTsid(schedule.getTsid())
				.userTsid(host.getTsid())
				.build());
	}

	@AfterEach
	void tearDown() {
		// 벌크 삭제(@Modifying)는 트랜잭션이 필요하고, 테스트 메서드는 스레드가 커밋된 데이터를 봐야 해서 트랜잭션이 없다
		transactionTemplate.executeWithoutResult(status -> {
			scheduleParticipantRepository.deleteAllByScheduleTsid(schedule.getTsid());
			scheduleRepository.deleteById(schedule.getTsid());
			usersRepository.deleteAll(guests);
			usersRepository.delete(host);
		});
	}

	@Test
	@DisplayName("남은 자리보다 많은 사용자가 동시에 참여해도 정확히 남은 자리만큼만 성공한다")
	void concurrentJoinsNeverExceedCapacity() throws Exception {
		// given: 모든 스레드가 동시에 출발하도록 래치로 묶는다
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
		CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<ErrorCode>> results = new ArrayList<>();

		for (UsersEntity guest : guests) {
			results.add(executor.submit(() -> {
				ready.countDown();
				start.await();
				try {
					scheduleService.joinSchedule(schedule.getTsid(), guest.getTsid());
					return null;
				} catch (BusinessException e) {
					return e.getErrorCode();
				}
			}));
		}

		// when
		ready.await(10, TimeUnit.SECONDS);
		start.countDown();
		executor.shutdown();
		assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

		// then
		long successCount = 0;
		List<ErrorCode> failures = new ArrayList<>();
		for (Future<ErrorCode> result : results) {
			ErrorCode errorCode = result.get();
			if (errorCode == null) {
				successCount++;
			} else {
				failures.add(errorCode);
			}
		}

		int remainingSeats = MAX_PARTICIPANTS - 1;
		assertThat(successCount).isEqualTo(remainingSeats);
		assertThat(failures).hasSize(CONCURRENT_USERS - remainingSeats)
			.containsOnly(ErrorCode.SCHEDULE_CAPACITY_EXCEEDED);
		assertThat(scheduleParticipantRepository.countByScheduleTsid(schedule.getTsid()))
			.isEqualTo(MAX_PARTICIPANTS);
	}

	private UsersEntity user(String prefix) {
		return UsersEntity.builder()
			.email(prefix + "-" + System.nanoTime() + "@example.com")
			.name(prefix)
			.build();
	}
}
