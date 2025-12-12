package com.gathering.auth.domain;

import com.gathering.user.domain.model.OAuthProvider;

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
	private final Boolean emailVerified;
	private final String name;
	private final String profileImageUrl;
}