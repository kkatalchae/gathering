package com.gathering.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth 콜백 요청 DTO
 * Google에서 리다이렉트로 받은 code와 state를 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCallbackRequest {

	@NotBlank(message = "Authorization code는 필수입니다.")
	private String code;

	@NotBlank(message = "State는 필수입니다.")
	private String state;
}