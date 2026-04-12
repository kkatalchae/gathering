package com.gathering.gathering.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;

public interface GatheringParticipantRepository extends JpaRepository<GatheringParticipantEntity, String> {

	Optional<GatheringParticipantEntity> findByGatheringTsidAndUserTsidAndRole(
		String gatheringTsid, String userTsid, ParticipantRole role);
}