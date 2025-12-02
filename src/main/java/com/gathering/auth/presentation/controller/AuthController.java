package com.gathering.auth.presentation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.application.dto.AuthTokens;
import com.gathering.auth.application.exception.InvalidTokenException;
import com.gathering.auth.infra.AuthConstants;
import com.gathering.auth.infra.CookieUtil;
import com.gathering.auth.presentation.dto.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
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
		AuthTokens tokens = authService.login(request);
		CookieUtil.setAuthTokens(response, tokens, (int)accessTokenValidityInSeconds,
			(int)refreshTokenValidityInSeconds);
		return ResponseEntity.ok().build();
	}

	/**
	 * 토큰 갱신 API
	 * RefreshToken을 검증하고 새로운 AccessToken과 RefreshToken 발급 (Token Rotation)
	 */
	@PostMapping("/refresh")
	public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = CookieUtil.getCookie(request, AuthConstants.REFRESH_TOKEN_COOKIE)
			.orElseThrow(() -> new InvalidTokenException("RefreshToken이 없습니다"));

		AuthTokens tokens = authService.refresh(refreshToken);
		CookieUtil.setAuthTokens(response, tokens, (int)accessTokenValidityInSeconds,
			(int)refreshTokenValidityInSeconds);

		return ResponseEntity.ok().build();
	}
}