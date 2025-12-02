package com.gathering.auth.infra;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 상수 정의
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthConstants {

	/**
	 * 쿠키 이름
	 */
	public static final String ACCESS_TOKEN_COOKIE = "accessToken";
	public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
}