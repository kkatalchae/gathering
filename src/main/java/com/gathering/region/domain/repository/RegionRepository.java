package com.gathering.region.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.region.domain.model.RegionEntity;

public interface RegionRepository extends JpaRepository<RegionEntity, String> {
}
