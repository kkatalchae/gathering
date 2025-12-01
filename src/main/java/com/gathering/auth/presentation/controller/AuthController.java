package com.gathering.auth.presentation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.application.dto.AuthTokens;
import com.gathering.auth.infra.CookieUtil;
import com.gathering.auth.presentation.dto.LoginRequest;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 인증 컨트롤러
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CookieUtil cookieUtil;

	@Value("${jwt.access-token-validity-in-seconds}")
	private long accessTokenValidityInSeconds;

	@Value("${jwt.refresh-token-validity-in-seconds}")
	private long refreshTokenValidityInSeconds;

	/**
	 * 로그인 API
	 * 인증 성공 시 JWT 토큰을 HTTP-only 쿠키로 전달
	 */
	@PostMapping("/login")
	public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		// 1. 인증 및 토큰 발급
		AuthTokens tokens = authService.login(request);

		// 2. 쿠키에 토큰 설정
		cookieUtil.addSecureCookie(response, "accessToken", tokens.getAccessToken(),
			(int)accessTokenValidityInSeconds);
		cookieUtil.addSecureCookie(response, "refreshToken", tokens.getRefreshToken(),
			(int)refreshTokenValidityInSeconds);

		// 3. 성공 응답 (Body 없이 200 OK)
		return ResponseEntity.ok().build();
	}
}