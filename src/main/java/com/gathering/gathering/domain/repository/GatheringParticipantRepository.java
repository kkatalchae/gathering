package com.gathering.gathering.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;

public interface GatheringParticipantRepository extends JpaRepository<GatheringParticipantEntity, String> {
}