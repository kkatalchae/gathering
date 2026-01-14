package com.gathering.gathering.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;

public interface GatheringParticipantRepository extends JpaRepository<GatheringParticipantEntity, String> {

	@Query("""
		SELECT gp FROM GatheringParticipantEntity gp
		JOIN FETCH gp.user
		WHERE gp.gatheringTsid = :gatheringTsid
		ORDER BY gp.role ASC, gp.joinedAt ASC
		""")
	List<GatheringParticipantEntity> findAllByGatheringTsidWithUser(@Param("gatheringTsid") String gatheringTsid);

	Optional<GatheringParticipantEntity> findByGatheringTsidAndUserTsid(String gatheringTsid, String userTsid);

	void deleteAllByGatheringTsid(String gatheringTsid);
}
