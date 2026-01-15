package com.gathering.gathering.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.policy.GatheringPolicy;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.GatheringListRequest;
import com.gathering.gathering.presentation.dto.GatheringListResponse;
import com.gathering.gathering.presentation.dto.GatheringResponse;
import com.gathering.gathering.presentation.dto.JoinGatheringResponse;
import com.gathering.gathering.presentation.dto.UpdateGatheringRequest;
import com.gathering.region.domain.model.RegionEntity;

/**
 * GatheringService 테스트
 */
@ExtendWith(MockitoExtension.class)
class GatheringServiceTest {

	@Mock
	private GatheringRepository gatheringRepository;

	@Mock
	private GatheringParticipantRepository participantRepository;

	@Mock
	private GatheringPolicy gatheringPolicy;

	@Mock
	private GatheringParticipantService gatheringParticipantService;

	@InjectMocks
	private GatheringService gatheringService;

	@Test
	@DisplayName("모임 생성에 성공하고 생성자가 OWNER로 등록된다")
	void createGatheringSuccess() {
		// given
		String ownerTsid = "01HQABCDEFGHJK";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("테스트 모임")
			.description("테스트 설명")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.mainImageUrl("https://example.com/image.jpg")
			.build();

		GatheringEntity savedGathering = GatheringEntity.builder()
			.tsid("01HQGATHERING1")
			.name(request.getName())
			.description(request.getDescription())
			.regionTsid(request.getRegionTsid())
			.category(request.getCategory())
			.mainImageUrl(request.getMainImageUrl())
			.maxParticipants(100)
			.createdAt(java.time.Instant.now())
			.build();

		given(gatheringRepository.save(any(GatheringEntity.class))).willReturn(savedGathering);

		// when
		GatheringResponse response = gatheringService.createGathering(ownerTsid, request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getTsid()).isEqualTo("01HQGATHERING1");
		assertThat(response.getName()).isEqualTo("테스트 모임");
		assertThat(response.getMaxParticipants()).isEqualTo(100);

		// Policy 검증 호출 확인
		then(gatheringPolicy).should().validateRegionExists(request.getRegionTsid());

		// Repository 호출 확인
		then(gatheringRepository).should().save(any(GatheringEntity.class));
		then(participantRepository).should().save(argThat(participant ->
			participant.getGatheringTsid().equals(savedGathering.getTsid())
				&& participant.getUserTsid().equals(ownerTsid)
				&& participant.getRole() == ParticipantRole.OWNER
		));
	}

	@Test
	@DisplayName("모임 이름이 null이면 Value Object에서 예외가 발생한다")
	void createGatheringWithNullName() {
		// given
		String ownerTsid = "01HQABCDEFGHJK";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name(null)
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.build();

		// when & then
		assertThatThrownBy(() -> gatheringService.createGathering(ownerTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NAME_REQUIRED);
	}

	@Test
	@DisplayName("모임 이름이 25자를 초과하면 Value Object에서 예외가 발생한다")
	void createGatheringWithTooLongName() {
		// given
		String ownerTsid = "01HQABCDEFGHJK";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("a".repeat(26))
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.build();

		// when & then
		assertThatThrownBy(() -> gatheringService.createGathering(ownerTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NAME_TOO_LONG);
	}

	@Test
	@DisplayName("모임 설명이 1000자를 초과하면 Value Object에서 예외가 발생한다")
	void createGatheringWithTooLongDescription() {
		// given
		String ownerTsid = "01HQABCDEFGHJK";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("테스트 모임")
			.description("a".repeat(1001))
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.build();

		// when & then
		assertThatThrownBy(() -> gatheringService.createGathering(ownerTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_DESCRIPTION_TOO_LONG);
	}

	@Test
	@DisplayName("존재하지 않는 지역이면 Policy에서 예외가 발생한다")
	void createGatheringWithInvalidRegion() {
		// given
		String ownerTsid = "01HQABCDEFGHJK";
		String invalidRegionTsid = "INVALID_REGION";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("테스트 모임")
			.regionTsid(invalidRegionTsid)
			.category(GatheringCategory.SPORTS)
			.build();

		willThrow(new BusinessException(ErrorCode.REGION_NOT_FOUND))
			.given(gatheringPolicy).validateRegionExists(invalidRegionTsid);

		// when & then
		assertThatThrownBy(() -> gatheringService.createGathering(ownerTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REGION_NOT_FOUND);
	}

	@Test
	@DisplayName("부모 지역으로 필터링 시 자식 지역의 모임도 포함하여 조회된다")
	void getGatheringsWithParentRegionIncludesChildren() {
		// given: 서울(부모) 지역 준비
		RegionEntity seoul = RegionEntity.builder()
			.tsid("0R1G1N0000001")
			.code("11")
			.name("서울특별시")
			.path("11")
			.depth(1)
			.createdAt(Instant.now())
			.build();

		RegionEntity gangnam = RegionEntity.builder()
			.tsid("0R1G1N0000010")
			.code("11680")
			.name("강남구")
			.path("11/11680")
			.depth(2)
			.createdAt(Instant.now())
			.build();

		// given: 강남구에 등록된 모임 준비
		GatheringEntity gatheringInGangnam = GatheringEntity.builder()
			.tsid("01HQGATHERING1")
			.name("강남 모임")
			.regionTsid(gangnam.getTsid())
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.region(gangnam)
			.build();

		// given: 서울 TSID로 필터링 요청
		GatheringListRequest request = GatheringListRequest.builder()
			.regionTsids(List.of(seoul.getTsid()))
			.size(20)
			.build();

		// Mock: Repository 쿼리가 계층적 필터링을 처리하여 강남 모임 반환
		given(gatheringRepository.findGatheringsWithFilters(
			any(),
			eq(List.of(seoul.getTsid())),
			any(),
			any(Pageable.class)
		)).willReturn(List.of(gatheringInGangnam));

		// when: 모임 목록 조회
		GatheringListResponse response = gatheringService.getGatherings(request);

		// then: 강남구 모임이 결과에 포함된다
		assertThat(response.getGatherings()).hasSize(1);
		assertThat(response.getGatherings().getFirst().getRegionName()).isEqualTo("강남구");

		// Repository 쿼리가 서울 TSID로 호출되었는지 검증
		then(gatheringRepository).should().findGatheringsWithFilters(
			any(),
			eq(List.of(seoul.getTsid())),
			any(),
			any(Pageable.class)
		);
	}

	@Test
	@DisplayName("OWNER가 모임 수정에 성공한다")
	void updateGatheringSuccessAsOwner() {
		// given: 모임과 수정 요청 준비
		String gatheringTsid = "01HQGATHERING1";
		String ownerTsid = "01HQOWNER12345";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("기존 모임")
			.description("기존 설명")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.mainImageUrl("https://example.com/old.jpg")
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.description("수정된 설명")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.mainImageUrl("https://example.com/new.jpg")
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));

		// when: 모임 수정
		GatheringResponse response = gatheringService.updateGathering(gatheringTsid, ownerTsid, request);

		// then: 수정된 정보 확인
		assertThat(response.getName()).isEqualTo("수정된 모임");
		assertThat(response.getDescription()).isEqualTo("수정된 설명");
		assertThat(response.getRegionTsid()).isEqualTo("01HQREGION5678");
		assertThat(response.getCategory()).isEqualTo(GatheringCategory.STUDY);

		// Policy 검증 호출 확인
		then(gatheringPolicy).should().validateUpdatePermission(gatheringTsid, ownerTsid);
		then(gatheringPolicy).should().validateRegionExists(request.getRegionTsid());
	}

	@Test
	@DisplayName("ADMIN이 모임 수정에 성공한다")
	void updateGatheringSuccessAsAdmin() {
		// given: 모임과 ADMIN 수정 요청 준비
		String gatheringTsid = "01HQGATHERING1";
		String adminTsid = "01HQADMIN12345";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("기존 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));

		// when: 모임 수정
		GatheringResponse response = gatheringService.updateGathering(gatheringTsid, adminTsid, request);

		// then: 수정 성공 확인
		assertThat(response.getName()).isEqualTo("수정된 모임");
		then(gatheringPolicy).should().validateUpdatePermission(gatheringTsid, adminTsid);
	}

	@Test
	@DisplayName("존재하지 않는 모임 수정 시 예외가 발생한다")
	void updateGatheringNotFound() {
		// given: 존재하지 않는 모임
		String invalidTsid = "INVALID_TSID";
		String userTsid = "01HQUSER123456";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		given(gatheringRepository.findById(invalidTsid)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> gatheringService.updateGathering(invalidTsid, userTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NOT_FOUND);
	}

	@Test
	@DisplayName("권한이 없는 사용자가 수정 시 Policy에서 예외가 발생한다")
	void updateGatheringPermissionDenied() {
		// given: 모임과 권한 없는 사용자
		String gatheringTsid = "01HQGATHERING1";
		String memberTsid = "01HQMEMBER1234";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("기존 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));
		willThrow(new BusinessException(ErrorCode.GATHERING_PERMISSION_DENIED))
			.given(gatheringPolicy).validateUpdatePermission(gatheringTsid, memberTsid);

		// when & then
		assertThatThrownBy(() -> gatheringService.updateGathering(gatheringTsid, memberTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_PERMISSION_DENIED);
	}

	@Test
	@DisplayName("수정 시 모임 이름이 25자를 초과하면 Value Object에서 예외가 발생한다")
	void updateGatheringWithTooLongName() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQUSER123456";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("기존 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("a".repeat(26))
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));

		// when & then
		assertThatThrownBy(() -> gatheringService.updateGathering(gatheringTsid, userTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NAME_TOO_LONG);
	}

	@Test
	@DisplayName("수정 시 존재하지 않는 지역이면 Policy에서 예외가 발생한다")
	void updateGatheringWithInvalidRegion() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQUSER123456";
		String invalidRegionTsid = "INVALID_REGION";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("기존 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid(invalidRegionTsid)
			.category(GatheringCategory.STUDY)
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));
		willThrow(new BusinessException(ErrorCode.REGION_NOT_FOUND))
			.given(gatheringPolicy).validateRegionExists(invalidRegionTsid);

		// when & then
		assertThatThrownBy(() -> gatheringService.updateGathering(gatheringTsid, userTsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REGION_NOT_FOUND);
	}

	@Test
	@DisplayName("OWNER가 모임 삭제에 성공한다")
	void deleteGatheringSuccessAsOwner() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String ownerTsid = "01HQOWNER12345";

		given(gatheringRepository.existsById(gatheringTsid)).willReturn(true);

		// when
		gatheringService.deleteGathering(gatheringTsid, ownerTsid);

		// then: Policy, Repository 호출 확인
		then(gatheringPolicy).should().validateOwnerPermission(gatheringTsid, ownerTsid);
		then(participantRepository).should().deleteAllByGatheringTsid(gatheringTsid);
		then(gatheringRepository).should().deleteById(gatheringTsid);
	}

	@Test
	@DisplayName("존재하지 않는 모임 삭제 시 예외가 발생한다")
	void deleteGatheringNotFound() {
		// given
		String invalidTsid = "INVALID_TSID";
		String userTsid = "01HQUSER123456";

		given(gatheringRepository.existsById(invalidTsid)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> gatheringService.deleteGathering(invalidTsid, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NOT_FOUND);
	}

	@Test
	@DisplayName("권한이 없는 사용자가 삭제 시 Policy에서 예외가 발생한다")
	void deleteGatheringPermissionDenied() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String adminTsid = "01HQADMIN12345";

		given(gatheringRepository.existsById(gatheringTsid)).willReturn(true);
		willThrow(new BusinessException(ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED))
			.given(gatheringPolicy).validateOwnerPermission(gatheringTsid, adminTsid);

		// when & then
		assertThatThrownBy(() -> gatheringService.deleteGathering(gatheringTsid, adminTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED);
	}

	// ==================== 모임 참여 테스트 ====================

	@Test
	@DisplayName("유효한 사용자가 모임에 참여하면 MEMBER로 참여자가 생성된다")
	void joinGatheringSuccessfully() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQUSER123456";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("테스트 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		GatheringParticipantEntity savedParticipant = GatheringParticipantEntity.builder()
			.tsid("01HQPARTIC1234")
			.gatheringTsid(gatheringTsid)
			.userTsid(userTsid)
			.role(ParticipantRole.MEMBER)
			.joinedAt(Instant.now())
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));
		given(participantRepository.existsByGatheringTsidAndUserTsid(gatheringTsid, userTsid)).willReturn(false);
		given(participantRepository.countByGatheringTsid(gatheringTsid)).willReturn(10L);
		given(participantRepository.save(any(GatheringParticipantEntity.class))).willReturn(savedParticipant);

		// when
		JoinGatheringResponse response = gatheringService.joinGathering(gatheringTsid, userTsid);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getGatheringTsid()).isEqualTo(gatheringTsid);
		assertThat(response.getUserTsid()).isEqualTo(userTsid);
		assertThat(response.getRole()).isEqualTo(ParticipantRole.MEMBER);

		then(participantRepository).should().save(argThat(participant ->
			participant.getGatheringTsid().equals(gatheringTsid)
				&& participant.getUserTsid().equals(userTsid)
				&& participant.getRole() == ParticipantRole.MEMBER
		));
	}

	@Test
	@DisplayName("존재하지 않는 모임에 참여하면 예외가 발생한다")
	void joinGatheringThrowsWhenGatheringNotFound() {
		// given
		String invalidGatheringTsid = "INVALID_TSID";
		String userTsid = "01HQUSER123456";

		given(gatheringRepository.findById(invalidGatheringTsid)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> gatheringService.joinGathering(invalidGatheringTsid, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NOT_FOUND);
	}

	@Test
	@DisplayName("이미 참여중인 모임에 참여하면 예외가 발생한다")
	void joinGatheringThrowsWhenAlreadyJoined() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQUSER123456";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("테스트 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));
		given(participantRepository.existsByGatheringTsidAndUserTsid(gatheringTsid, userTsid)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> gatheringService.joinGathering(gatheringTsid, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_JOINED_GATHERING);
	}

	@Test
	@DisplayName("정원이 가득 찬 모임에 참여하면 예외가 발생한다")
	void joinGatheringThrowsWhenCapacityExceeded() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQUSER123456";

		GatheringEntity gathering = GatheringEntity.builder()
			.tsid(gatheringTsid)
			.name("테스트 모임")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		given(gatheringRepository.findById(gatheringTsid)).willReturn(Optional.of(gathering));
		given(participantRepository.existsByGatheringTsidAndUserTsid(gatheringTsid, userTsid)).willReturn(false);
		given(participantRepository.countByGatheringTsid(gatheringTsid)).willReturn(100L);

		// when & then
		assertThatThrownBy(() -> gatheringService.joinGathering(gatheringTsid, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_CAPACITY_EXCEEDED);
	}

	// ==================== 모임 퇴장 테스트 ====================

	@Test
	@DisplayName("MEMBER가 모임에서 퇴장하면 참여자 정보가 삭제된다")
	void leaveGatheringSuccessfully() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQMEMBER1234";

		GatheringParticipantEntity participant = GatheringParticipantEntity.builder()
			.tsid("01HQPARTIC1234")
			.gatheringTsid(gatheringTsid)
			.userTsid(userTsid)
			.role(ParticipantRole.MEMBER)
			.joinedAt(Instant.now())
			.build();

		given(gatheringRepository.existsById(gatheringTsid)).willReturn(true);
		given(gatheringParticipantService.findParticipants(gatheringTsid, userTsid)).willReturn(participant);

		// when
		gatheringService.leaveGathering(gatheringTsid, userTsid);

		// then
		then(participantRepository).should().delete(participant);
	}

	@Test
	@DisplayName("ADMIN이 모임에서 퇴장하면 참여자 정보가 삭제된다")
	void leaveGatheringAsAdminSuccessfully() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String adminTsid = "01HQADMIN12345";

		GatheringParticipantEntity participant = GatheringParticipantEntity.builder()
			.tsid("01HQPARTIC5678")
			.gatheringTsid(gatheringTsid)
			.userTsid(adminTsid)
			.role(ParticipantRole.ADMIN)
			.joinedAt(Instant.now())
			.build();

		given(gatheringRepository.existsById(gatheringTsid)).willReturn(true);
		given(gatheringParticipantService.findParticipants(gatheringTsid, adminTsid)).willReturn(participant);

		// when
		gatheringService.leaveGathering(gatheringTsid, adminTsid);

		// then
		then(participantRepository).should().delete(participant);
	}

	@Test
	@DisplayName("존재하지 않는 모임에서 퇴장하면 예외가 발생한다")
	void leaveGatheringThrowsWhenGatheringNotFound() {
		// given
		String invalidGatheringTsid = "INVALID_TSID";
		String userTsid = "01HQUSER123456";

		given(gatheringRepository.existsById(invalidGatheringTsid)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> gatheringService.leaveGathering(invalidGatheringTsid, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_NOT_FOUND);
	}

	@Test
	@DisplayName("참여하지 않은 모임에서 퇴장하면 예외가 발생한다")
	void leaveGatheringThrowsWhenNotParticipant() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String userTsid = "01HQUSER123456";

		given(gatheringRepository.existsById(gatheringTsid)).willReturn(true);
		given(gatheringParticipantService.findParticipants(gatheringTsid, userTsid))
			.willThrow(new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> gatheringService.leaveGathering(gatheringTsid, userTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTICIPANT_NOT_FOUND);
	}

	@Test
	@DisplayName("OWNER가 퇴장하면 예외가 발생한다")
	void leaveGatheringThrowsWhenOwner() {
		// given
		String gatheringTsid = "01HQGATHERING1";
		String ownerTsid = "01HQOWNER12345";

		GatheringParticipantEntity ownerParticipant = GatheringParticipantEntity.builder()
			.tsid("01HQPARTIC0001")
			.gatheringTsid(gatheringTsid)
			.userTsid(ownerTsid)
			.role(ParticipantRole.OWNER)
			.joinedAt(Instant.now())
			.build();

		given(gatheringRepository.existsById(gatheringTsid)).willReturn(true);
		given(gatheringParticipantService.findParticipants(gatheringTsid, ownerTsid)).willReturn(ownerParticipant);

		// when & then
		assertThatThrownBy(() -> gatheringService.leaveGathering(gatheringTsid, ownerTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.OWNER_CANNOT_LEAVE_GATHERING);
	}
}
