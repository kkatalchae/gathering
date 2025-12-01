package com.gathering.auth.application;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.auth.application.dto.AuthTokens;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.auth.presentation.dto.LoginRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * 로그인 처리
	 * Spring Security의 AuthenticationManager를 사용하여 인증 수행
	 * 인증 성공 시 JWT 토큰 발급
	 */
	@Transactional(readOnly = true)
	public AuthTokens login(LoginRequest request) {
		// 1. 인증 시도
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				request.getEmail(),
				request.getPassword()
			)
		);

		// 2. 인증 성공 시 사용자 정보 추출
		String email = authentication.getName();

		// 3. JWT 토큰 생성
		String accessToken = jwtTokenProvider.createAccessToken(email);
		String refreshToken = jwtTokenProvider.createRefreshToken(email);

		// 4. 토큰 반환
		return AuthTokens.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}
}