package com.gathering.auth.infra;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.ResponseCookie;

import com.gathering.auth.application.dto.AuthTokens;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 쿠키 생성 및 관리 유틸리티
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CookieUtil {

	private static final String COOKIE_PATH = "/";
	private static final String SAME_SITE = "Lax";

	/**
	 * HTTP-only 보안 쿠키 생성
	 *
	 * @param response HttpServletResponse
	 * @param name 쿠키 이름
	 * @param value 쿠키 값
	 * @param maxAge 쿠키 만료 시간 (초)
	 */
	public static void addSecureCookie(HttpServletResponse response, String name, String value, int maxAge) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
			.httpOnly(true)          // JavaScript 접근 방지 (XSS 방어)
			.secure(true)            // HTTPS만 전송
			.path(COOKIE_PATH)       // 모든 경로에서 사용
			.maxAge(maxAge)          // 만료 시간
			.sameSite(SAME_SITE)     // CSRF 방어
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
	}

	/**
	 * 쿠키 삭제 (만료 시간을 0으로 설정)
	 *
	 * @param response HttpServletResponse
	 * @param name 쿠키 이름
	 */
	public static void deleteCookie(HttpServletResponse response, String name) {
		addSecureCookie(response, name, "", 0);
	}

	/**
	 * 쿠키에서 값 조회
	 *
	 * @param request HttpServletRequest
	 * @param name 쿠키 이름
	 * @return Optional로 감싼 쿠키 값
	 */
	public static Optional<String> getCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}

		return Arrays.stream(cookies)
			.filter(cookie -> name.equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst();
	}

	/**
	 * 인증 토큰들을 쿠키에 설정
	 *
	 * @param response HttpServletResponse
	 * @param tokens 인증 토큰 (AccessToken, RefreshToken)
	 * @param accessTokenValidityInSeconds AccessToken 만료 시간 (초)
	 * @param refreshTokenValidityInSeconds RefreshToken 만료 시간 (초)
	 */
	public static void setAuthTokens(HttpServletResponse response, AuthTokens tokens,
		int accessTokenValidityInSeconds, int refreshTokenValidityInSeconds) {
		addSecureCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE, tokens.getAccessToken(),
			accessTokenValidityInSeconds);
		addSecureCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE, tokens.getRefreshToken(),
			refreshTokenValidityInSeconds);
	}
}