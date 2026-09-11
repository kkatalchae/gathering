package com.gathering.schedule.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.schedule.domain.model.ScheduleParticipantCount;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipantEntity, String> {

	long countByScheduleTsid(String scheduleTsid);

	boolean existsByScheduleTsidAndUserTsid(String scheduleTsid, String userTsid);

	Optional<ScheduleParticipantEntity> findByScheduleTsidAndUserTsid(String scheduleTsid, String userTsid);

	/**
	 * 일정 참여자를 사용자 정보와 함께 참여 순으로 조회
	 */
	@Query("""
		SELECT sp FROM ScheduleParticipantEntity sp
		JOIN FETCH sp.user
		WHERE sp.scheduleTsid = :scheduleTsid
		ORDER BY sp.joinedAt ASC, sp.tsid ASC
		""")
	List<ScheduleParticipantEntity> findAllByScheduleTsidWithUser(@Param("scheduleTsid") String scheduleTsid);

	/**
	 * 여러 일정의 참여 인원을 한 번의 쿼리로 집계 (목록 조회 시 일정별 반복 조회 방지)
	 * 참여자가 없는 일정은 결과에 포함되지 않는다
	 */
	@Query("""
		SELECT new com.gathering.schedule.domain.model.ScheduleParticipantCount(sp.scheduleTsid, COUNT(sp))
		FROM ScheduleParticipantEntity sp
		WHERE sp.scheduleTsid IN :scheduleTsids
		GROUP BY sp.scheduleTsid
		""")
	List<ScheduleParticipantCount> countByScheduleTsidIn(@Param("scheduleTsids") List<String> scheduleTsids);

	/**
	 * 일정 참여자 일괄 삭제
	 * 벌크 연산이므로 실행 전 flush, 실행 후 영속성 컨텍스트를 비워 stale 엔티티를 남기지 않는다
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM ScheduleParticipantEntity sp WHERE sp.scheduleTsid = :scheduleTsid")
	void deleteAllByScheduleTsid(@Param("scheduleTsid") String scheduleTsid);

	/**
	 * 여러 일정의 참여자를 한 번의 쿼리로 일괄 삭제 (모임 삭제 시 사용)
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM ScheduleParticipantEntity sp WHERE sp.scheduleTsid IN :scheduleTsids")
	void deleteAllByScheduleTsidIn(@Param("scheduleTsids") List<String> scheduleTsids);
}
