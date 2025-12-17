package com.gathering.auth.application;

import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.user.domain.model.OAuthProvider;

/**
 * OAuth 서비스 인터페이스
 * 각 OAuth 제공자별 구현체가 이 인터페이스를 구현
 */
public interface OAuthService {

	/**
	 * OAuth 인증 URL 생성
	 *
	 * @param state CSRF 방지를 위한 state 파라미터
	 * @return 인증 URL
	 */
	String generateAuthorizationUrl(String state);

	/**
	 * Authorization Code를 사용하여 Access Token 획득 후 사용자 정보 조회
	 *
	 * @param code Authorization Code
	 * @param state CSRF 방지를 위한 state 파라미터
	 * @return OAuthUserInfo
	 */
	OAuthUserInfo getUserInfo(String code, String state);

	/**
	 * 이 서비스가 처리하는 OAuth 제공자
	 *
	 * @return OAuthProvider
	 */
	OAuthProvider getProvider();
}