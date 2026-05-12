package com.gathering.auth.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO (OAuth 2.0 스타일)
 * - accessToken: 응답 본문에 포함 (클라이언트가 localStorage에 저장)
 * - refreshToken: HTTP-only 쿠키로 전달 (본문에 포함 안 됨)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

	private String accessToken;
	private String tokenType;  // "Bearer"
	private Long expiresIn;    // 초 단위
}