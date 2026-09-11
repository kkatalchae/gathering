package com.gathering.gathering.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;

import jakarta.persistence.LockModeType;

public interface GatheringRepository extends JpaRepository<GatheringEntity, String> {

	/**
	 * 모임 row 에 쓰기 락을 걸고 조회
	 * 참여(정원 확인 → 저장)와 삭제처럼 모임 참여자를 바꾸는 쓰기 경로는 반드시 이 메서드로 시작한다
	 * 근거와 락 순서 규칙은 docs/adr/0001-capacity-concurrency-control.md 참고
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT g FROM GatheringEntity g WHERE g.tsid = :tsid")
	Optional<GatheringEntity> findByTsidForUpdate(@Param("tsid") String tsid);

	@Query("""
		SELECT g FROM GatheringEntity g
		JOIN FETCH g.region r
		WHERE (:categories IS NULL OR g.category IN :categories)
		AND (:regionTsids IS NULL
		OR EXISTS (
		SELECT 1 FROM RegionEntity parent
		WHERE parent.tsid IN :regionTsids
		AND r.path LIKE CONCAT(parent.path, '%')
		))
		AND (:cursor IS NULL OR g.tsid < :cursor)
		ORDER BY g.createdAt DESC, g.tsid DESC
		""")
	List<GatheringEntity> findGatheringsWithFilters(
		@Param("categories") List<GatheringCategory> categories,
		@Param("regionTsids") List<String> regionTsids,
		@Param("cursor") String cursor,
		Pageable pageable
	);
}
