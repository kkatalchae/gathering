package com.gathering.user.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.gathering.auth.domain.OAuthUserInfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
 * 사용자 소셜 계정 연동 정보 엔티티
 *
 * 복합 PK: (user_tsid, provider)
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(UserOAuthConnectionEntity.ConnectionId.class)
@Table(
	name = "user_oauth_connections",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_provider_user", columnNames = {"provider", "provider_id"})
	},
	indexes = {
		@Index(name = "idx_user_tsid", columnList = "user_tsid")
	}
)
public class UserOAuthConnectionEntity {

	@Id
	@Column(name = "user_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String userTsid;

	@Id
	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private OAuthProvider provider;

	@Column(name = "provider_id", nullable = false)
	private String providerId;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "user_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_user_oauth_user_tsid")
	)
	private UsersEntity user;

	/**
	 * 소셜 연동 정보 생성 팩토리 메서드
	 */
	public static UserOAuthConnectionEntity of(
		String userTsid,
		OAuthProvider provider,
		String providerId,
		String email
	) {
		return UserOAuthConnectionEntity.builder()
			.userTsid(userTsid)
			.provider(provider)
			.providerId(providerId)
			.email(email)
			.build();
	}

	/**
	 * OAuthUserInfo로부터 연동 정보 생성
	 */
	public static UserOAuthConnectionEntity from(String userTsid, OAuthUserInfo oAuthUserInfo) {
		return of(
			userTsid,
			oAuthUserInfo.getProvider(),
			oAuthUserInfo.getProviderId(),
			oAuthUserInfo.getEmail()
		);
	}

	/**
	 * 복합 PK 클래스
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class ConnectionId implements Serializable {

		private String userTsid;
		private OAuthProvider provider;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConnectionId that = (ConnectionId)o;
			return Objects.equals(userTsid, that.userTsid) && provider == that.provider;
		}

		@Override
		public int hashCode() {
			return Objects.hash(userTsid, provider);
		}
	}
}
