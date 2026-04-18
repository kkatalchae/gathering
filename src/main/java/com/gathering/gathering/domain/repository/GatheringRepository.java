package com.gathering.gathering.domain.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;

public interface GatheringRepository extends JpaRepository<GatheringEntity, String> {

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
