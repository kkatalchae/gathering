package com.gathering.region.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.region.presentation.dto.RegionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegionService {

	private final RegionRepository regionRepository;

	@Transactional(readOnly = true)
	public List<RegionResponse> getAllRegionsHierarchical() {
		List<RegionEntity> allRegions = regionRepository.findAll();

		// depth별로 그룹화
		Map<Integer, List<RegionEntity>> regionsByDepth = allRegions.stream()
			.collect(Collectors.groupingBy(RegionEntity::getDepth));

		// depth-2 지역을 부모 코드별로 그룹화
		List<RegionEntity> depth2Regions = regionsByDepth.getOrDefault(2, List.of());
		Map<String, List<RegionResponse>> childrenByParentCode = depth2Regions.stream()
			.collect(Collectors.groupingBy(
				region -> region.getPath().split("/")[0],
				Collectors.mapping(RegionResponse::fromLeaf, Collectors.toList())
			));

		// depth-1 지역에 children 연결
		List<RegionEntity> depth1Regions = regionsByDepth.getOrDefault(1, List.of());
		return depth1Regions.stream()
			.map(parent -> {
				List<RegionResponse> children = childrenByParentCode.getOrDefault(
					parent.getCode(),
					List.of()
				);
				return RegionResponse.from(parent, children);
			})
			.toList();
	}
}