package com.gathering.gathering.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;

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
import com.gathering.gathering.presentation.dto.CreateGatheringResponse;
import com.gathering.gathering.presentation.dto.GatheringListRequest;
import com.gathering.gathering.presentation.dto.GatheringListResponse;
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
		CreateGatheringResponse response = gatheringService.createGathering(ownerTsid, request);

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
			participant.getGatheringTsid().equals(savedGathering.getTsid()) &&
				participant.getUserTsid().equals(ownerTsid) &&
				participant.getRole() == ParticipantRole.OWNER
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
		assertThat(response.getGatherings().get(0).getRegionName()).isEqualTo("강남구");

		// Repository 쿼리가 서울 TSID로 호출되었는지 검증
		then(gatheringRepository).should().findGatheringsWithFilters(
			any(),
			eq(List.of(seoul.getTsid())),
			any(),
			any(Pageable.class)
		);
	}
}