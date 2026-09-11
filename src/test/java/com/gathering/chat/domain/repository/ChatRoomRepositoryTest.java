package com.gathering.chat.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.model.ChatRoomType;
import com.gathering.common.config.JpaAuditingConfig;
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * ChatRoomRepository 테스트 — 주체(모임/일정)당 채팅방 하나라는 제약과 FK 를 실제 DB 로 검증한다
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ChatRoomRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private GatheringRepository gatheringRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private UsersRepository usersRepository;

	private GatheringEntity gathering;
	private ScheduleEntity schedule;

	@BeforeEach
	void setUp() {
		UsersEntity host = usersRepository.save(UsersEntity.builder().email("host@example.com").name("호스트").build());
		RegionEntity region = regionRepository.save(RegionEntity.builder()
			.name("서울특별시").code("11").depth(1).path("/11/").build());
		gathering = gatheringRepository.save(GatheringEntity.builder()
			.name("한강 러닝크루").regionTsid(region.getTsid()).category(GatheringCategory.SPORTS).build());
		schedule = scheduleRepository.save(ScheduleEntity.builder()
			.title("9월 정기 모임").startAt(Instant.parse("2026-09-20T10:00:00Z"))
			.locationName("한강공원 2주차장").createdBy(host.getTsid()).build());
	}

	@Test
	@DisplayName("모임 채팅방과 일정 채팅방을 각각의 주체 TSID 로 찾을 수 있다")
	void findByOwner() {
		// given
		chatRoomRepository.save(ChatRoomEntity.forGathering(gathering.getTsid()));
		chatRoomRepository.save(ChatRoomEntity.forSchedule(schedule.getTsid()));
		entityManager.flush();
		entityManager.clear();

		// when & then
		assertThat(chatRoomRepository.findByGatheringTsid(gathering.getTsid()))
			.isPresent().get()
			.extracting(ChatRoomEntity::getRoomType).isEqualTo(ChatRoomType.GATHERING);
		assertThat(chatRoomRepository.findByScheduleTsid(schedule.getTsid()))
			.isPresent().get()
			.extracting(ChatRoomEntity::getRoomType).isEqualTo(ChatRoomType.SCHEDULE);
	}

	@Test
	@DisplayName("같은 모임에 채팅방을 두 개 만들 수 없다")
	void oneRoomPerGathering() {
		// given
		chatRoomRepository.save(ChatRoomEntity.forGathering(gathering.getTsid()));
		entityManager.flush();

		// when & then: 리포지토리를 거쳐야 Hibernate 예외가 Spring 예외로 번역된다
		assertThatThrownBy(() -> chatRoomRepository.saveAndFlush(ChatRoomEntity.forGathering(gathering.getTsid())))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("존재하지 않는 모임에는 채팅방을 만들 수 없다")
	void gatheringMustExist() {
		// when & then
		assertThatThrownBy(() -> chatRoomRepository.saveAndFlush(ChatRoomEntity.forGathering("UNKNOWN_TSID00")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
