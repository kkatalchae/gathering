package com.gathering.auth.infra;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 상수 정의
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthConstants {

	/**
	 * Authorization 헤더 관련
	 */
	public static final String BEARER_PREFIX = "Bearer ";

	/**
	 * 쿠키 이름
	 */
	public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
}
