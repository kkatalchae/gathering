package com.gathering.region.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.region.domain.model.RegionEntity;

public interface RegionRepository extends JpaRepository<RegionEntity, String> {

	/**
	 * path로 시작하는 모든 지역 조회 (계층적 필터링용)
	 * 예: "11" -> "11", "11/11680", "11/11740" 등 모두 조회
	 */
	List<RegionEntity> findByPathStartingWith(String pathPrefix);
}
