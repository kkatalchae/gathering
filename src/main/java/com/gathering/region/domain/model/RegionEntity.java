package com.gathering.region.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "regions",
	indexes = {
		@Index(name = "idx_region_path", columnList = "path"),
		@Index(name = "idx_region_depth", columnList = "depth"),
		@Index(name = "idx_region_code", columnList = "code")
	}
)
public class RegionEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(nullable = false, length = 10, unique = true)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, length = 100)
	private String path;

	@Column(nullable = false)
	private Integer depth;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;
}
