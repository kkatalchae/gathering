package com.gathering.user.presentation.dto;

import com.gathering.common.annotation.AesEncrypted;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청 DTO
 * PUT /users/me/password API에서 사용
 * 현재 비밀번호 확인 후 새 비밀번호로 변경
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangePasswordRequest {

	/**
	 * 현재 비밀번호 (AES 암호화)
	 * 현재 비밀번호를 확인하여 본인 인증
	 */
	@NotNull(message = "현재 비밀번호를 입력해주세요")
	@AesEncrypted
	private String currentPassword;

	/**
	 * 새 비밀번호 (AES 암호화)
	 * 비밀번호 정책: 최소 8자, 숫자 + 특수문자 포함
	 */
	@NotNull(message = "새 비밀번호를 입력해주세요")
	@AesEncrypted
	private String newPassword;
}