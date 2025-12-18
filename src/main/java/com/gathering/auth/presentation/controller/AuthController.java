package com.gathering.auth.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.presentation.dto.LoginRequest;
import com.gathering.auth.presentation.dto.LoginResponse;
import com.gathering.auth.presentation.dto.RefreshResponse;

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
	private final AuthenticationManager authenticationManager;

	/**
	 * 로그인 API (OAuth 2.0 스타일)
	 * - AccessToken: 응답 본문에 포함
	 * - RefreshToken: HTTP-only 쿠키로 설정
	 */
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
		HttpServletResponse response) {

		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				request.getEmail(),
				request.getPassword()
			)
		);

		return ResponseEntity.ok(authService.login(request, response));
	}

	/**
	 * 토큰 갱신 API (OAuth 2.0 스타일)
	 * RefreshToken을 검증하고 새로운 AccessToken 발급
	 * - AccessToken: 응답 본문에 포함
	 * - RefreshToken: 변경 없음 (90일 유효기간)
	 */
	@PostMapping("/refresh")
	public ResponseEntity<RefreshResponse> refresh(HttpServletRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	/**
	 * 로그아웃 API
	 * Redis에서 RefreshToken을 삭제하여 토큰 갱신 불가능하도록 만듦
	 * 특정 기기만 로그아웃 (멀티 디바이스 지원)
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		authService.logout(request, response);
		return ResponseEntity.ok().build();
	}
}
