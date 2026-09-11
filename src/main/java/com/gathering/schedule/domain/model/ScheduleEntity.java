package com.gathering.schedule.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.user.domain.model.UsersEntity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

/**
 * 일정 엔티티
 * gatheringTsid가 있으면 모임에 귀속된 일정, null이면 일회성 독립 일정이다
 * 정원(maxParticipants)이 null이면 인원 제한이 없다
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "schedules",
	indexes = {
		@Index(name = "idx_schedule_gathering_start_at", columnList = "gathering_tsid, start_at"),
		@Index(name = "idx_schedule_start_at", columnList = "start_at")
	}
)
public class ScheduleEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	/** null이면 모임에 귀속되지 않은 독립 일정 */
	@Column(name = "gathering_tsid", length = 13, columnDefinition = "CHAR(13)")
	private String gatheringTsid;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at")
	private Instant endAt;

	@Column(name = "location_name", nullable = false, length = 100)
	private String locationName;

	@Column(name = "location_address", length = 255)
	private String locationAddress;

	/** null이면 인원 제한 없음 */
	@Column(name = "max_participants")
	private Integer maxParticipants;

	@Column(name = "created_by", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	@Column(name = "updated_at")
	@LastModifiedDate
	private Instant updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "gathering_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_schedule_gathering")
	)
	private GatheringEntity gathering;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "created_by",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_schedule_creator")
	)
	private UsersEntity creator;

	/**
	 * 모임 귀속 일정 여부
	 *
	 * @return 모임에 귀속된 일정이면 true, 독립 일정이면 false
	 */
	public boolean isAttachedToGathering() {
		return gatheringTsid != null;
	}

	/**
	 * 해당 사용자가 일정을 개설한 호스트인지 여부
	 *
	 * @param userTsid 확인할 사용자 TSID
	 * @return 호스트이면 true
	 */
	public boolean isHostedBy(String userTsid) {
		return createdBy.equals(userTsid);
	}

	/**
	 * 일정 정보 수정
	 *
	 * @param title 일정 제목
	 * @param description 일정 설명
	 * @param startAt 시작 시각
	 * @param endAt 종료 시각 (null 허용)
	 * @param locationName 장소명 (필수)
	 * @param locationAddress 장소 주소 (null 허용)
	 * @param maxParticipants 정원 (null이면 제한 없음)
	 */
	public void update(String title, String description, Instant startAt, Instant endAt,
		String locationName, String locationAddress, Integer maxParticipants) {
		this.title = title;
		this.description = description;
		this.startAt = startAt;
		this.endAt = endAt;
		this.locationName = locationName;
		this.locationAddress = locationAddress;
		this.maxParticipants = maxParticipants;
	}
}
