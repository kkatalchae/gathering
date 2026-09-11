package com.gathering.gathering.application;

import static org.assertj.core.api.Assertions.*;

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
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * 모임 참여 동시성 테스트 (#50)
 * 남은 자리보다 많은 사용자가 동시에 참여를 시도해도 정확히 남은 자리만큼만 성공해야 한다
 * 근거: docs/adr/0001-capacity-concurrency-control.md
 */
@SpringBootTest
class GatheringJoinConcurrencyTest {

	private static final int MAX_PARTICIPANTS = 3;
	private static final int CONCURRENT_USERS = 10;

	@Autowired
	private GatheringService gatheringService;

	@Autowired
	private GatheringRepository gatheringRepository;

	@Autowired
	private GatheringParticipantRepository participantRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private UsersEntity owner;
	private List<UsersEntity> guests;
	private RegionEntity region;
	private GatheringEntity gathering;

	@BeforeEach
	void setUp() {
		owner = usersRepository.save(user("owner"));
		guests = new ArrayList<>();
		for (int i = 0; i < CONCURRENT_USERS; i++) {
			guests.add(usersRepository.save(user("guest" + i)));
		}
		region = regionRepository.save(RegionEntity.builder()
			.name("동시성 테스트 지역")
			.code("CC" + System.nanoTime() % 100000)
			.depth(1)
			.path("/cc/")
			.build());

		// OWNER 가 이미 1자리를 차지하므로 남은 자리는 MAX_PARTICIPANTS - 1
		gathering = gatheringRepository.save(GatheringEntity.builder()
			.name("선착순 모임")
			.regionTsid(region.getTsid())
			.category(GatheringCategory.SPORTS)
			.maxParticipants(MAX_PARTICIPANTS)
			.build());
		participantRepository.save(GatheringParticipantEntity.builder()
			.gatheringTsid(gathering.getTsid())
			.userTsid(owner.getTsid())
			.role(ParticipantRole.OWNER)
			.build());
	}

	@AfterEach
	void tearDown() {
		transactionTemplate.executeWithoutResult(status -> {
			participantRepository.deleteAllByGatheringTsid(gathering.getTsid());
			gatheringRepository.deleteById(gathering.getTsid());
			regionRepository.delete(region);
			usersRepository.deleteAll(guests);
			usersRepository.delete(owner);
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
					gatheringService.joinGathering(gathering.getTsid(), guest.getTsid());
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
			.containsOnly(ErrorCode.GATHERING_CAPACITY_EXCEEDED);
		assertThat(participantRepository.countByGatheringTsid(gathering.getTsid()))
			.isEqualTo(MAX_PARTICIPANTS);
	}

	private UsersEntity user(String prefix) {
		return UsersEntity.builder()
			.email(prefix + "-" + System.nanoTime() + "@example.com")
			.name(prefix)
			.build();
	}
}
