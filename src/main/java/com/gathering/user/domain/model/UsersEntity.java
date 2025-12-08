package com.gathering.user.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_email", columnNames = "email"),
		@UniqueConstraint(name = "uk_user_phone_number", columnNames = "phone_number")
	}
)
public class UsersEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(nullable = false, length = 320)
	private String email;

	@Column
	private String nickname;

	@Column(nullable = false)
	private String name;

	// TODO 추후 문자를 통해 검증
	@Column(nullable = false)
	private String phoneNumber;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Builder.Default
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	/**
	 * 프로필 정보 업데이트
	 * JPA 엔티티이므로 필드를 직접 변경하면 dirty checking으로 자동 UPDATE
	 *
	 * @param nickname 닉네임 (null이면 변경하지 않음)
	 * @param name 이름 (null이면 변경하지 않음)
	 * @param phoneNumber 전화번호 (null이면 변경하지 않음)
	 */
	public void updateProfile(String nickname, String name, String phoneNumber) {
		if (nickname != null) {
			this.nickname = nickname;
		}
		if (name != null) {
			this.name = name;
		}
		if (phoneNumber != null) {
			this.phoneNumber = phoneNumber;
		}
	}
}
