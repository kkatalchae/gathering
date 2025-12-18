package com.gathering.auth.domain;

import java.util.Map;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.UsersEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * OAuth 제공자에서 받은 사용자 정보
 * 불변 객체 (Value Object)
 */
@Getter
@Builder
public class OAuthUserInfo {

	private final OAuthProvider provider;
	private final String providerId; // OAuth 제공자의 고유 사용자 ID (Google: sub, Kakao: id 등)
	private final String email;
	private final String name;
	private final String profileImageUrl;

	public static OAuthUserInfo of(String provider, Map<String, Object> attributes) {
		return switch (provider) {
			case "google" -> ofGoogle(attributes);
			default -> throw new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
		};
	}

	private static OAuthUserInfo ofGoogle(Map<String, Object> attributes) {
		return OAuthUserInfo.builder()
			.provider(OAuthProvider.GOOGLE)
			.providerId((String)attributes.get("sub"))
			.email((String)attributes.get("email"))
			.name((String)attributes.get("name"))
			.profileImageUrl((String)attributes.get("picture"))
			.build();
	}

	public static UsersEntity toUsersEntity(OAuthUserInfo oAuthUserInfo) {
		return UsersEntity.builder()
			.email(oAuthUserInfo.getEmail())
			.name(oAuthUserInfo.getName())
			.profileImageUrl(oAuthUserInfo.getProfileImageUrl())
			.emailVerified(true)
			.build();
	}
}
