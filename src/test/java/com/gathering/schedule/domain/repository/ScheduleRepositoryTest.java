package com.gathering.schedule.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleParticipantCount;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * ScheduleRepository 커서 페이지네이션 쿼리 테스트
 * 같은 시작 시각을 가진 일정이 페이지 경계에 걸쳐도 누락/중복 없이 이어지는지 실제 DB로 검증한다
 */
@DataJpaTest
class ScheduleRepositoryTest {

	private static final Instant BASE_TIME = Instant.parse("2026-09-15T00:00:00Z");
	private static final Pageable PAGE_OF_TWO = PageRequest.of(0, 2);

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private GatheringRepository gatheringRepository;

	private UsersEntity host;
	private GatheringEntity gathering;

	@BeforeEach
	void setUp() {
		host = usersRepository.save(UsersEntity.builder()
			.email("host@example.com")
			.name("호스트")
			.build());

		RegionEntity region = regionRepository.save(RegionEntity.builder()
			.name("서울특별시")
			.code("11")
			.depth(1)
			.path("/11/")
			.build());

		gathering = gatheringRepository.save(GatheringEntity.builder()
			.name("한강 러닝크루")
			.regionTsid(region.getTsid())
			.category(GatheringCategory.SPORTS)
			.build());
	}

	@Test
	@DisplayName("다가오는 일정은 시작 시각 오름차순으로 조회되고 지난 일정은 제외된다")
	void findUpcomingSchedulesOrdersByStartAt() {
		// given
		ScheduleEntity past = saveSchedule("지난 일정", BASE_TIME.minus(Duration.ofDays(1)), null);
		ScheduleEntity later = saveSchedule("나중 일정", BASE_TIME.plus(Duration.ofDays(3)), null);
		ScheduleEntity sooner = saveSchedule("가까운 일정", BASE_TIME.plus(Duration.ofDays(1)), null);

		flushAndClear();

		// when
		List<ScheduleEntity> result = scheduleRepository.findUpcomingSchedules(
			null, BASE_TIME, null, null, PageRequest.of(0, 10));

		// then
		assertThat(result).extracting(ScheduleEntity::getTsid)
			.containsExactly(sooner.getTsid(), later.getTsid())
			.doesNotContain(past.getTsid());
	}

	@Test
	@DisplayName("같은 시작 시각의 일정이 페이지 경계에 걸려도 커서로 이어서 조회하면 누락과 중복이 없다")
	void findUpcomingSchedulesWithCursorAcrossSameStartAt() {
		// given: 같은 시각 일정 3개 + 다른 시각 1개
		Instant sameTime = BASE_TIME.plus(Duration.ofDays(1));
		saveSchedule("동시 A", sameTime, null);
		saveSchedule("동시 B", sameTime, null);
		saveSchedule("동시 C", sameTime, null);
		saveSchedule("다음날", sameTime.plus(Duration.ofDays(1)), null);

		flushAndClear();

		// when: 2개씩 커서로 끝까지 순회
		List<ScheduleEntity> firstPage = scheduleRepository.findUpcomingSchedules(
			null, BASE_TIME, null, null, PAGE_OF_TWO);
		ScheduleEntity firstPageLast = firstPage.getLast();
		List<ScheduleEntity> secondPage = scheduleRepository.findUpcomingSchedules(
			null, BASE_TIME, firstPageLast.getStartAt(), firstPageLast.getTsid(), PAGE_OF_TWO);
		ScheduleEntity secondPageLast = secondPage.getLast();
		List<ScheduleEntity> thirdPage = scheduleRepository.findUpcomingSchedules(
			null, BASE_TIME, secondPageLast.getStartAt(), secondPageLast.getTsid(), PAGE_OF_TWO);

		// then
		assertThat(firstPage).hasSize(2);
		assertThat(secondPage).hasSize(2);
		assertThat(thirdPage).isEmpty();

		List<String> allTsids = new ArrayList<>();
		firstPage.forEach(schedule -> allTsids.add(schedule.getTsid()));
		secondPage.forEach(schedule -> allTsids.add(schedule.getTsid()));
		assertThat(allTsids).doesNotHaveDuplicates().hasSize(4);
	}

	@Test
	@DisplayName("모임 TSID로 필터링하면 해당 모임 일정만 조회된다")
	void findUpcomingSchedulesFilteredByGathering() {
		// given
		ScheduleEntity attached = saveSchedule("모임 일정", BASE_TIME.plus(Duration.ofDays(1)), gathering.getTsid());
		saveSchedule("독립 일정", BASE_TIME.plus(Duration.ofDays(1)), null);

		flushAndClear();

		// when
		List<ScheduleEntity> result = scheduleRepository.findUpcomingSchedules(
			gathering.getTsid(), BASE_TIME, null, null, PageRequest.of(0, 10));

		// then
		assertThat(result).extracting(ScheduleEntity::getTsid).containsExactly(attached.getTsid());
		assertThat(result.getFirst().getGathering().getName()).isEqualTo("한강 러닝크루");
	}

	@Test
	@DisplayName("지난 일정은 시작 시각 내림차순으로 조회되고 커서로 이어진다")
	void findPastSchedulesOrdersByStartAtDescWithCursor() {
		// given
		ScheduleEntity threeDaysAgo = saveSchedule("3일 전", BASE_TIME.minus(Duration.ofDays(3)), null);
		ScheduleEntity oneDayAgo = saveSchedule("1일 전", BASE_TIME.minus(Duration.ofDays(1)), null);
		ScheduleEntity twoDaysAgo = saveSchedule("2일 전", BASE_TIME.minus(Duration.ofDays(2)), null);
		saveSchedule("미래", BASE_TIME.plus(Duration.ofDays(1)), null);

		flushAndClear();

		// when
		List<ScheduleEntity> firstPage = scheduleRepository.findPastSchedules(
			null, BASE_TIME, null, null, PAGE_OF_TWO);
		ScheduleEntity firstPageLast = firstPage.getLast();
		List<ScheduleEntity> secondPage = scheduleRepository.findPastSchedules(
			null, BASE_TIME, firstPageLast.getStartAt(), firstPageLast.getTsid(), PAGE_OF_TWO);

		// then
		assertThat(firstPage).extracting(ScheduleEntity::getTsid)
			.containsExactly(oneDayAgo.getTsid(), twoDaysAgo.getTsid());
		assertThat(secondPage).extracting(ScheduleEntity::getTsid)
			.containsExactly(threeDaysAgo.getTsid());
	}

	@Test
	@DisplayName("여러 일정의 참여 인원을 한 번에 집계하며 참여자가 없는 일정은 결과에 없다")
	void countByScheduleTsidIn() {
		// given
		ScheduleEntity withTwo = saveSchedule("참여 2명", BASE_TIME.plus(Duration.ofDays(1)), null);
		ScheduleEntity withNone = saveSchedule("참여 0명", BASE_TIME.plus(Duration.ofDays(1)), null);
		UsersEntity another = usersRepository.save(UsersEntity.builder()
			.email("another@example.com")
			.name("참여자")
			.build());
		saveParticipant(withTwo, host);
		saveParticipant(withTwo, another);

		flushAndClear();

		// when
		List<ScheduleParticipantCount> counts = scheduleParticipantRepository.countByScheduleTsidIn(
			List.of(withTwo.getTsid(), withNone.getTsid()));

		// then
		assertThat(counts).hasSize(1);
		assertThat(counts.getFirst().scheduleTsid()).isEqualTo(withTwo.getTsid());
		assertThat(counts.getFirst().participantCount()).isEqualTo(2L);
	}

	/**
	 * 저장한 엔티티가 1차 캐시에 남아 있으면 쿼리가 fetch join 결과 대신 캐시 인스턴스를 돌려주므로,
	 * 실제 요청처럼 DB에서 새로 읽도록 조회 전에 컨텍스트를 비운다
	 */
	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private ScheduleEntity saveSchedule(String title, Instant startAt, String gatheringTsid) {
		return scheduleRepository.save(ScheduleEntity.builder()
			.gatheringTsid(gatheringTsid)
			.title(title)
			.startAt(startAt)
			.locationName("한강공원 2주차장")
			.createdBy(host.getTsid())
			.build());
	}

	private void saveParticipant(ScheduleEntity schedule, UsersEntity user) {
		scheduleParticipantRepository.save(ScheduleParticipantEntity.builder()
			.scheduleTsid(schedule.getTsid())
			.userTsid(user.getTsid())
			.build());
	}
}
