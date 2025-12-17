package com.gathering.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth 계정 연동 요청 DTO
 * 로그인한 사용자가 OAuth 계정을 연동할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLinkRequest {

	@NotBlank(message = "Authorization code는 필수입니다.")
	private String code;

	@NotBlank(message = "State는 필수입니다.")
	private String state;
}