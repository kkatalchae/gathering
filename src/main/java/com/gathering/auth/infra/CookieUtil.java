package com.gathering.auth.infra;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 쿠키 생성 및 관리 유틸리티
 */
@Component
public class CookieUtil {

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
	public void addSecureCookie(HttpServletResponse response, String name, String value, int maxAge) {
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
	public void deleteCookie(HttpServletResponse response, String name) {
		addSecureCookie(response, name, "", 0);
	}
}