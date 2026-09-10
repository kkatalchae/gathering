package com.gathering.schedule.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 참여자 엔티티
 * 참여 취소는 row 삭제로 처리하므로 상태 컬럼을 두지 않는다
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "schedule_participants",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_schedule_user", columnNames = {"schedule_tsid", "user_tsid"})
	},
	indexes = {
		@Index(name = "idx_schedule_participant_user", columnList = "user_tsid"),
		@Index(name = "idx_schedule_participant_schedule", columnList = "schedule_tsid")
	}
)
public class ScheduleParticipantEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(name = "schedule_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String scheduleTsid;

	@Column(name = "user_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String userTsid;

	@Column(name = "joined_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant joinedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "schedule_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_schedule_participant_schedule")
	)
	private ScheduleEntity schedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "user_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_schedule_participant_user")
	)
	private UsersEntity user;
}
