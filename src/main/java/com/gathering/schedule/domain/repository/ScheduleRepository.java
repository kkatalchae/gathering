package com.gathering.schedule.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.schedule.domain.model.ScheduleEntity;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, String> {

	/**
	 * 일정을 호스트, 모임 정보와 함께 조회
	 * 독립 일정은 모임이 없으므로 모임은 LEFT JOIN 으로 가져온다
	 */
	@Query("""
		SELECT s FROM ScheduleEntity s
		JOIN FETCH s.creator
		LEFT JOIN FETCH s.gathering
		WHERE s.tsid = :tsid
		""")
	Optional<ScheduleEntity> findByTsidWithCreatorAndGathering(@Param("tsid") String tsid);

	@Query("SELECT s.tsid FROM ScheduleEntity s WHERE s.gatheringTsid = :gatheringTsid")
	List<String> findTsidsByGatheringTsid(@Param("gatheringTsid") String gatheringTsid);

	/**
	 * 모임에 귀속된 일정 일괄 삭제
	 * 벌크 연산이므로 실행 전 flush, 실행 후 영속성 컨텍스트를 비워 stale 엔티티를 남기지 않는다
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM ScheduleEntity s WHERE s.gatheringTsid = :gatheringTsid")
	void deleteAllByGatheringTsid(@Param("gatheringTsid") String gatheringTsid);
}
