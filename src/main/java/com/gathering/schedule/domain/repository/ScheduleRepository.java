package com.gathering.schedule.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.schedule.domain.model.ScheduleEntity;

import jakarta.persistence.LockModeType;

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

	/**
	 * 기준 시각 이후에 시작하는 일정을 가까운 순으로 조회 (커서 기반 페이지네이션)
	 * 같은 시각의 일정은 tsid 순으로 정렬해 커서가 항상 유일한 위치를 가리키게 한다
	 *
	 * @param gatheringTsid 모임 TSID (null이면 모임 귀속 여부와 무관하게 전체 조회)
	 * @param baseTime 기준 시각 (이 시각 이후에 시작하는 일정만)
	 * @param cursorStartAt 커서의 시작 시각 (null이면 첫 페이지)
	 * @param cursorTsid 커서의 일정 TSID (null이면 첫 페이지)
	 */
	@Query("""
		SELECT s FROM ScheduleEntity s
		JOIN FETCH s.creator
		LEFT JOIN FETCH s.gathering
		WHERE (:gatheringTsid IS NULL OR s.gatheringTsid = :gatheringTsid)
		AND s.startAt >= :baseTime
		AND (:cursorStartAt IS NULL
		OR s.startAt > :cursorStartAt
		OR (s.startAt = :cursorStartAt AND s.tsid > :cursorTsid))
		ORDER BY s.startAt ASC, s.tsid ASC
		""")
	List<ScheduleEntity> findUpcomingSchedules(
		@Param("gatheringTsid") String gatheringTsid,
		@Param("baseTime") Instant baseTime,
		@Param("cursorStartAt") Instant cursorStartAt,
		@Param("cursorTsid") String cursorTsid,
		Pageable pageable
	);

	/**
	 * 기준 시각 이전에 시작한 일정을 최근 순으로 조회 (커서 기반 페이지네이션)
	 * 정렬이 내림차순이므로 커서 비교 방향도 반대다
	 *
	 * @param gatheringTsid 모임 TSID (null이면 모임 귀속 여부와 무관하게 전체 조회)
	 * @param baseTime 기준 시각 (이 시각 이전에 시작한 일정만)
	 * @param cursorStartAt 커서의 시작 시각 (null이면 첫 페이지)
	 * @param cursorTsid 커서의 일정 TSID (null이면 첫 페이지)
	 */
	@Query("""
		SELECT s FROM ScheduleEntity s
		JOIN FETCH s.creator
		LEFT JOIN FETCH s.gathering
		WHERE (:gatheringTsid IS NULL OR s.gatheringTsid = :gatheringTsid)
		AND s.startAt < :baseTime
		AND (:cursorStartAt IS NULL
		OR s.startAt < :cursorStartAt
		OR (s.startAt = :cursorStartAt AND s.tsid < :cursorTsid))
		ORDER BY s.startAt DESC, s.tsid DESC
		""")
	List<ScheduleEntity> findPastSchedules(
		@Param("gatheringTsid") String gatheringTsid,
		@Param("baseTime") Instant baseTime,
		@Param("cursorStartAt") Instant cursorStartAt,
		@Param("cursorTsid") String cursorTsid,
		Pageable pageable
	);

	/**
	 * 일정 row 에 쓰기 락을 걸고 조회
	 * 참여(정원 확인 → 저장), 정원 변경, 삭제처럼 일정 참여자를 바꾸는 쓰기 경로는 반드시 이 메서드로 시작한다
	 * 근거와 락 순서 규칙은 docs/adr/0001-capacity-concurrency-control.md 참고
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ScheduleEntity s WHERE s.tsid = :tsid")
	Optional<ScheduleEntity> findByTsidForUpdate(@Param("tsid") String tsid);

	/**
	 * 모임에 귀속된 일정들에 쓰기 락을 걸고 조회 (모임 삭제 시 사용)
	 * 진행 중인 일정 참여가 있으면 그 참여가 끝날 때까지 기다린 뒤 삭제를 시작하므로 락 순서가 뒤집히지 않는다
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ScheduleEntity s WHERE s.gatheringTsid = :gatheringTsid")
	List<ScheduleEntity> findAllByGatheringTsidForUpdate(@Param("gatheringTsid") String gatheringTsid);

	/**
	 * 모임에 귀속된 일정 일괄 삭제
	 * 벌크 연산이므로 실행 전 flush, 실행 후 영속성 컨텍스트를 비워 stale 엔티티를 남기지 않는다
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("DELETE FROM ScheduleEntity s WHERE s.gatheringTsid = :gatheringTsid")
	void deleteAllByGatheringTsid(@Param("gatheringTsid") String gatheringTsid);
}
