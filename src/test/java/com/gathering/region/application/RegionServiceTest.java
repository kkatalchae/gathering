package com.gathering.region.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.region.presentation.dto.RegionResponse;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

	@Mock
	private RegionRepository regionRepository;

	@InjectMocks
	private RegionService regionService;

	@Test
	@DisplayName("전체 지역을 계층 구조로 조회하고 부모-자식 관계가 올바르게 구성된다")
	void getAllRegionsHierarchicalSuccess() {
		// given: depth-1과 depth-2 지역 데이터를 준비
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

		RegionEntity gangdong = RegionEntity.builder()
			.tsid("0R1G1N0000011")
			.code("11740")
			.name("강동구")
			.path("11/11740")
			.depth(2)
			.createdAt(Instant.now())
			.build();

		RegionEntity gyeonggi = RegionEntity.builder()
			.tsid("0R1G1N0000002")
			.code("41")
			.name("경기도")
			.path("41")
			.depth(1)
			.createdAt(Instant.now())
			.build();

		RegionEntity suwon = RegionEntity.builder()
			.tsid("0R1G1N0000020")
			.code("41131")
			.name("수원시")
			.path("41/41131")
			.depth(2)
			.createdAt(Instant.now())
			.build();

		given(regionRepository.findAll()).willReturn(
			List.of(seoul, gangnam, gangdong, gyeonggi, suwon)
		);

		// when: 계층 구조로 지역을 조회
		List<RegionResponse> result = regionService.getAllRegionsHierarchical();

		// then: depth-1 지역이 2개이고 각각 자식을 올바르게 포함한다
		assertThat(result).hasSize(2);

		RegionResponse seoulResponse = result.stream()
			.filter(r -> r.getName().equals("서울특별시"))
			.findFirst()
			.orElseThrow();
		assertThat(seoulResponse.getChildren()).hasSize(2);
		assertThat(seoulResponse.getChildren())
			.extracting("name")
			.containsExactlyInAnyOrder("강남구", "강동구");

		RegionResponse gyeonggiResponse = result.stream()
			.filter(r -> r.getName().equals("경기도"))
			.findFirst()
			.orElseThrow();
		assertThat(gyeonggiResponse.getChildren()).hasSize(1);
		assertThat(gyeonggiResponse.getChildren().get(0).getName()).isEqualTo("수원시");
	}

	@Test
	@DisplayName("자식이 없는 부모 지역도 빈 배열로 올바르게 반환된다")
	void getAllRegionsWithNoChildrenReturnsEmptyArray() {
		// given: 자식이 없는 depth-1 지역만 존재
		RegionEntity seoul = RegionEntity.builder()
			.tsid("0R1G1N0000001")
			.code("11")
			.name("서울특별시")
			.path("11")
			.depth(1)
			.createdAt(Instant.now())
			.build();

		given(regionRepository.findAll()).willReturn(List.of(seoul));

		// when: 계층 구조로 지역을 조회
		List<RegionResponse> result = regionService.getAllRegionsHierarchical();

		// then: 자식이 빈 배열로 반환된다
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getChildren()).isEmpty();
	}
}