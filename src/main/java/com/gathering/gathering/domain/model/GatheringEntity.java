package com.gathering.gathering.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.gathering.region.domain.model.RegionEntity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
	name = "gatherings",
	indexes = {
		@Index(name = "idx_gathering_region", columnList = "region_tsid"),
		@Index(name = "idx_gathering_category", columnList = "category"),
		@Index(name = "idx_gathering_created_at", columnList = "created_at")
	}
)
public class GatheringEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "region_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String regionTsid;

	@Column(nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private GatheringCategory category;

	@Column(name = "main_image_url", length = 500)
	private String mainImageUrl;

	@Column(name = "max_participants", nullable = false)
	@Builder.Default
	private Integer maxParticipants = 100;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "region_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_gathering_region")
	)
	private RegionEntity region;
}
