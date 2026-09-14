package com.gathering.user.application;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
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
import com.gathering.schedule.domain.model.ScheduleEntity;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;
import com.gathering.schedule.domain.repository.ScheduleRepository;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * 회원 탈퇴 통합 테스트 (#48) — 실제 DB 위에서 "모임에 참여하고 일정을 열고 참여한 사용자" 의 탈퇴를 끝까지 돌린다
 * #48 의 원래 결함(FK 위반 500)은 mock 으로 재현할 수 없어 통합 테스트로 둔다
 */
@SpringBootTest
class UserWithdrawalIntegrationTest {

	@Autowired
	private UserWithdrawalService userWithdrawalService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private UserSecurityRepository userSecurityRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private GatheringRepository gatheringRepository;

	@Autowired
	private GatheringParticipantRepository gatheringParticipantRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private ScheduleParticipantRepository scheduleParticipantRepository;

	@Autowired
	private ChatRoomReadPositionRepository readPositionRepository;

	private UsersEntity owner;
	private UsersEntity member;
	private RegionEntity region;
	private GatheringEntity gathering;
	private ScheduleEntity memberUpcomingHosted;
	private ScheduleEntity memberPastHosted;
	private ScheduleEntity ownerUpcoming;
	private ScheduleEntity ownerPast;

	@BeforeEach
	void setUp() {
		owner = usersRepository.save(user("owner"));
		member = usersRepository.save(user("member"));
		userSecurityRepository.save(UserSecurityEntity.of(member.getTsid(), "$2a$10$hash"));
		region = regionRepository.save(RegionEntity.builder()
			.name("탈퇴 테스트 지역").code("WD" + System.nanoTime() % 100000).depth(1).path("/wd/").build());

		gathering = gatheringRepository.save(GatheringEntity.builder()
			.name("탈퇴 테스트 모임").regionTsid(region.getTsid()).category(GatheringCategory.SPORTS).build());
		joinGathering(owner, ParticipantRole.OWNER);
		joinGathering(member, ParticipantRole.MEMBER);

		Instant now = Instant.now();
		// member 가 호스트: 시작 전 1건(삭제 대상), 지난 것 1건(유지)
		memberUpcomingHosted = saveSchedule(member, now.plus(Duration.ofDays(1)));
		memberPastHosted = saveSchedule(member, now.minus(Duration.ofDays(1)));
		// owner 가 호스트, member 가 게스트로 참여: 시작 전 1건(참여 해제), 지난 것 1건(참여 기록 유지)
		ownerUpcoming = saveSchedule(owner, now.plus(Duration.ofDays(2)));
		ownerPast = saveSchedule(owner, now.minus(Duration.ofDays(2)));
		joinSchedule(ownerUpcoming, member);
		joinSchedule(ownerPast, member);
		joinSchedule(memberUpcomingHosted, owner);

		readPositionRepository.save(ChatRoomReadPositionEntity.of(member.getTsid(), "01HQROOMWD0001", "0HZZZZZZZZZZ1"));
	}

	@AfterEach
	void tearDown() {
		transactionTemplate.executeWithoutResult(status -> {
			for (ScheduleEntity schedule : scheduleRepository.findAll()) {
				scheduleParticipantRepository.deleteAllByScheduleTsid(schedule.getTsid());
			}
			scheduleRepository.deleteAll();
			gatheringParticipantRepository.deleteAllByGatheringTsid(gathering.getTsid());
			gatheringRepository.delete(gathering);
			regionRepository.delete(region);
			userSecurityRepository.deleteById(member.getTsid());
			usersRepository.deleteAll();
		});
		readPositionRepository.deleteByKeyUserTsid(member.getTsid());
	}

	@Test
	@DisplayName("모임 멤버이자 일정 호스트·참여자인 사용자가 탈퇴하면 연관 데이터가 정책대로 정리되고 row 는 익명화된다")
	void withdrawMemberCleansUpAndAnonymizes() {
		// when: #48 의 결함 재현 조건 — 이전 코드라면 fk_participant_user 위반으로 실패했다
		transactionTemplate.executeWithoutResult(status -> userWithdrawalService.withdraw(member.getTsid()));

		// then: 사용자 row 는 남되 익명화
		UsersEntity withdrawn = usersRepository.findById(member.getTsid()).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(withdrawn.getName()).isEqualTo(UsersEntity.WITHDRAWN_NAME);
		assertThat(withdrawn.getEmail()).endsWith("@" + UsersEntity.WITHDRAWN_EMAIL_DOMAIN);
		assertThat(userSecurityRepository.findById(member.getTsid())).isEmpty();

		// 모임 참여는 전부 해제, 다른 멤버는 그대로
		assertThat(gatheringParticipantRepository.existsByGatheringTsidAndUserTsid(gathering.getTsid(), member.getTsid()))
			.isFalse();
		assertThat(gatheringParticipantRepository.countByGatheringTsid(gathering.getTsid())).isEqualTo(1L);

		// 호스트인 시작 전 일정은 참여자와 함께 삭제, 지난 일정은 유지 (호스트 = 탈퇴한 사용자)
		assertThat(scheduleRepository.findById(memberUpcomingHosted.getTsid())).isEmpty();
		assertThat(scheduleParticipantRepository.countByScheduleTsid(memberUpcomingHosted.getTsid())).isZero();
		assertThat(scheduleRepository.findById(memberPastHosted.getTsid())).isPresent();

		// 참여 중인 시작 전 일정은 참여 해제, 지난 일정의 참여 기록은 유지
		assertThat(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(ownerUpcoming.getTsid(), member.getTsid()))
			.isFalse();
		assertThat(scheduleParticipantRepository.existsByScheduleTsidAndUserTsid(ownerPast.getTsid(), member.getTsid()))
			.isTrue();

		// Cassandra 읽음 위치는 커밋 후 삭제
		assertThat(readPositionRepository.findByKeyUserTsid(member.getTsid())).isEmpty();
	}

	@Test
	@DisplayName("오너인 모임이 있으면 탈퇴가 거부되고 아무 데이터도 바뀌지 않는다")
	void withdrawOwnerIsRejected() {
		// when & then
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
			userWithdrawalService.withdraw(owner.getTsid())))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_WITHDRAW_AS_GATHERING_OWNER);

		UsersEntity stillActive = usersRepository.findById(owner.getTsid()).orElseThrow();
		assertThat(stillActive.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(gatheringParticipantRepository.countByGatheringTsid(gathering.getTsid())).isEqualTo(2L);
		assertThat(scheduleRepository.findById(ownerUpcoming.getTsid())).isPresent();
	}

	private UsersEntity user(String prefix) {
		return UsersEntity.builder()
			.email(prefix + "-" + System.nanoTime() + "@example.com")
			.name(prefix)
			.build();
	}

	private void joinGathering(UsersEntity user, ParticipantRole role) {
		gatheringParticipantRepository.save(GatheringParticipantEntity.builder()
			.gatheringTsid(gathering.getTsid()).userTsid(user.getTsid()).role(role).build());
	}

	private ScheduleEntity saveSchedule(UsersEntity host, Instant startAt) {
		ScheduleEntity schedule = scheduleRepository.save(ScheduleEntity.builder()
			.gatheringTsid(gathering.getTsid())
			.title("탈퇴 테스트 일정")
			.startAt(startAt)
			.locationName("한강공원")
			.createdBy(host.getTsid())
			.build());
		joinSchedule(schedule, host);
		return schedule;
	}

	private void joinSchedule(ScheduleEntity schedule, UsersEntity user) {
		scheduleParticipantRepository.save(ScheduleParticipantEntity.builder()
			.scheduleTsid(schedule.getTsid()).userTsid(user.getTsid()).build());
	}
}
