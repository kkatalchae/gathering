package com.gathering.gathering.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.gathering.domain.model.GatheringEntity;

public interface GatheringRepository extends JpaRepository<GatheringEntity, String> {
}