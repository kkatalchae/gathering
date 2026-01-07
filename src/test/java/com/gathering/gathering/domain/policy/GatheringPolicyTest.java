package com.gathering.gathering.domain.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
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
}