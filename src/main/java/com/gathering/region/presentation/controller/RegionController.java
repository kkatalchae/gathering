package com.gathering.region.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.region.application.RegionService;
import com.gathering.region.presentation.dto.RegionResponse;

import lombok.RequiredArgsConstructor;

/**
 * 지역 API Controller
 */
@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

	private final RegionService regionService;

	/**
	 * 전체 지역 목록 조회 (계층 구조)
	 *
	 * @return 지역 목록 (depth-1 지역이 children 배열 포함)
	 */
	@GetMapping
	public ResponseEntity<List<RegionResponse>> getAllRegions() {
		List<RegionResponse> regions = regionService.getAllRegionsHierarchical();
		return ResponseEntity.ok(regions);
	}
}