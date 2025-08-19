package com.gathering.user.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "user_security"
)
public class UserSecurityEntity {

	@Id
	@Column(nullable = false, columnDefinition = "BIGINT UNSIGNED")
	private Long userTsid;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "failed_login_count", nullable = false)
	private int failedLoginCount;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "password_changed_at")
	private Instant passwordChangedAt;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "user_tsid",
		foreignKey = @ForeignKey(name = "fk_user_security_user_tsid")
	)
	private UsersEntity usersEntity;

	public static UserSecurityEntity of(Long userTsid, String passwordHash) {
		return UserSecurityEntity.builder()
			.userTsid(userTsid)
			.passwordHash(passwordHash)
			.failedLoginCount(0)
			.build();
	}
}
