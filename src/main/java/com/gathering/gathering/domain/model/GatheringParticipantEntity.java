package com.gathering.gathering.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.gathering.user.domain.model.UsersEntity;

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
import jakarta.persistence.UniqueConstraint;
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
	name = "gathering_participants",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_gathering_user", columnNames = {"gathering_tsid", "user_tsid"})
	},
	indexes = {
		@Index(name = "idx_participant_user", columnList = "user_tsid"),
		@Index(name = "idx_participant_gathering", columnList = "gathering_tsid")
	}
)
public class GatheringParticipantEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(name = "gathering_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String gatheringTsid;

	@Column(name = "user_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String userTsid;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ParticipantRole role;

	@Column(name = "joined_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant joinedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "gathering_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_participant_gathering")
	)
	private GatheringEntity gathering;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "user_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_participant_user")
	)
	private UsersEntity user;

	/**
	 * 역할 변경
	 *
	 * @param newRole 변경할 역할
	 */
	public void changeRole(ParticipantRole newRole) {
		this.role = newRole;
	}
}
