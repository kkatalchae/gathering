package com.gathering.auth.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 토큰 DTO (서비스 레이어 내부용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokens {

	private String accessToken;
	private String refreshToken;
}