package com.gathering.gathering.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;

public interface GatheringParticipantRepository extends JpaRepository<GatheringParticipantEntity, String> {

	@Query("""
		SELECT gp FROM GatheringParticipantEntity gp
		JOIN FETCH gp.user
		WHERE gp.gatheringTsid = :gatheringTsid
		ORDER BY
		  CASE WHEN gp.role = com.gathering.gathering.domain.model.ParticipantRole.OWNER THEN 1
		       WHEN gp.role = com.gathering.gathering.domain.model.ParticipantRole.ADMIN THEN 2
		       ELSE 3 END ASC,
		  gp.joinedAt ASC
		""")
	List<GatheringParticipantEntity> findAllByGatheringTsidWithUser(@Param("gatheringTsid") String gatheringTsid);

	Optional<GatheringParticipantEntity> findByGatheringTsidAndUserTsid(String gatheringTsid, String userTsid);

	void deleteAllByGatheringTsid(String gatheringTsid);

	long countByGatheringTsid(String gatheringTsid);

	boolean existsByGatheringTsidAndUserTsid(String gatheringTsid, String userTsid);

	/**
	 * 주어진 사용자들 중 해당 모임에 참여중인 사용자의 TSID 만 반환
	 * 일정 참여자 목록에서 모임 멤버/게스트를 구분할 때 한 번의 쿼리로 판정하기 위해 사용
	 */
	@Query("""
		SELECT gp.userTsid FROM GatheringParticipantEntity gp
		WHERE gp.gatheringTsid = :gatheringTsid
		AND gp.userTsid IN :userTsids
		""")
	List<String> findUserTsidsByGatheringTsidAndUserTsidIn(
		@Param("gatheringTsid") String gatheringTsid,
		@Param("userTsids") List<String> userTsids);

	Optional<GatheringParticipantEntity> findByGatheringTsidAndUserTsidAndRole(
		String gatheringTsid, String userTsid, ParticipantRole role);
}
