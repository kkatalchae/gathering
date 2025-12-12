package com.gathering.user.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * OAuth 제공자 연동 정보
 * 한 사용자가 여러 OAuth 제공자를 동시에 연동할 수 있음 (Google + Kakao + Naver)
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "oauth_providers",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_oauth_provider_user",
			columnNames = {"provider", "provider_user_id"}
		)
	},
	indexes = {
		@Index(name = "idx_oauth_user_tsid", columnList = "user_tsid")
	}
)
public class OAuthProviderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_tsid", nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String userTsid;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private OAuthProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 255)
	private String providerUserId;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "profile_image_url", length = 2048)
	private String profileImageUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	private Instant updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "user_tsid",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(name = "fk_oauth_provider_user_tsid")
	)
	private UsersEntity user;

	/**
	 * OAuth 제공자 정보 생성
	 *
	 * @param userTsid 사용자 TSID
	 * @param provider OAuth 제공자
	 * @param providerUserId OAuth 제공자의 고유 사용자 ID
	 * @param email OAuth 제공자에서 제공한 이메일
	 * @param profileImageUrl 프로필 이미지 URL
	 * @return OAuthProviderEntity
	 */
	public static OAuthProviderEntity of(
		String userTsid,
		OAuthProvider provider,
		String providerUserId,
		String email,
		String profileImageUrl
	) {
		return OAuthProviderEntity.builder()
			.userTsid(userTsid)
			.provider(provider)
			.providerUserId(providerUserId)
			.email(email)
			.profileImageUrl(profileImageUrl)
			.build();
	}
}