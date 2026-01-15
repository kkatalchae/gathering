package com.gathering.gathering.domain.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.application.GatheringParticipantService;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.region.domain.repository.RegionRepository;

/**
 * GatheringPolicy Domain Policy 테스트
 */
@ExtendWith(MockitoExtension.class)
class GatheringPolicyTest {

	@Mock
	private RegionRepository regionRepository;

	@InjectMocks
	private GatheringPolicy gatheringPolicy;

	@Mock
	private GatheringParticipantService gatheringParticipantService;

	@Test
	@DisplayName("존재하는 지역 TSID는 검증을 통과한다")
	void validateRegionExistsSuccess() {
		// given
		String validRegionTsid = "01HQABCDEFGHJK";
		given(regionRepository.existsById(validRegionTsid)).willReturn(true);

		// when & then
		assertThatCode(() -> gatheringPolicy.validateRegionExists(validRegionTsid))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("존재하지 않는 지역 TSID는 예외가 발생한다")
	void validateRegionExistsFailure() {
		// given
		String invalidRegionTsid = "INVALID_TSID";
		given(regionRepository.existsById(invalidRegionTsid)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> gatheringPolicy.validateRegionExists(invalidRegionTsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REGION_NOT_FOUND);
	}

	@Nested
	@DisplayName("validateDeletePermission 테스트")
	class ValidateDeletePermissionTest {

		@Test
		@DisplayName("OWNER는 삭제 권한 검증을 통과한다")
		void validateDeletePermissionSuccessAsOwner() {
			// given
			String gatheringTsid = "01HQGATHERING1";
			String ownerTsid = "01HQOWNER12345";
			GatheringParticipantEntity owner = GatheringParticipantEntity.builder()
				.gatheringTsid(gatheringTsid)
				.userTsid(ownerTsid)
				.role(ParticipantRole.OWNER)
				.build();

			given(gatheringParticipantService.findParticipants(gatheringTsid, ownerTsid))
				.willReturn(owner);

			// when & then
			assertThatCode(() -> gatheringPolicy.validateOwnerPermission(gatheringTsid, ownerTsid))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("ADMIN은 삭제 권한 검증에서 예외가 발생한다")
		void validateDeletePermissionFailAsAdmin() {
			// given
			String gatheringTsid = "01HQGATHERING1";
			String adminTsid = "01HQADMIN12345";
			GatheringParticipantEntity admin = GatheringParticipantEntity.builder()
				.gatheringTsid(gatheringTsid)
				.userTsid(adminTsid)
				.role(ParticipantRole.ADMIN)
				.build();

			given(gatheringParticipantService.findParticipants(gatheringTsid, adminTsid))
				.willReturn(admin);

			// when & then
			assertThatThrownBy(() -> gatheringPolicy.validateOwnerPermission(gatheringTsid, adminTsid))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED);
		}

		@Test
		@DisplayName("MEMBER는 삭제 권한 검증에서 예외가 발생한다")
		void validateDeletePermissionFailAsMember() {
			// given
			String gatheringTsid = "01HQGATHERING1";
			String memberTsid = "01HQMEMBER1234";
			GatheringParticipantEntity member = GatheringParticipantEntity.builder()
				.gatheringTsid(gatheringTsid)
				.userTsid(memberTsid)
				.role(ParticipantRole.MEMBER)
				.build();

			given(gatheringParticipantService.findParticipants(gatheringTsid, memberTsid))
				.willReturn(member);

			// when & then
			assertThatThrownBy(() -> gatheringPolicy.validateOwnerPermission(gatheringTsid, memberTsid))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED);
		}

		@Test
		@DisplayName("비참여자는 삭제 권한 검증에서 예외가 발생한다")
		void validateDeletePermissionFailAsNonParticipant() {
			// given
			String gatheringTsid = "01HQGATHERING1";
			String nonParticipantTsid = "01HQNONPARTICIP";

			given(gatheringParticipantService.findParticipants(gatheringTsid, nonParticipantTsid)).willThrow(
				new BusinessException(ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED));

			// when & then
			assertThatThrownBy(() -> gatheringPolicy.validateOwnerPermission(gatheringTsid, nonParticipantTsid))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.GATHERING_OWNER_PERMISSION_NEEDED);
		}
	}
}
