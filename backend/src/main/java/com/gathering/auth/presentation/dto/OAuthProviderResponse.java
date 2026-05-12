package com.gathering.auth.presentation.dto;

import java.time.Instant;

import com.gathering.user.domain.model.OAuthProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연동된 OAuth 제공자 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthProviderResponse {

	private OAuthProvider provider;
	private String email;
	private Instant linkedAt;
}