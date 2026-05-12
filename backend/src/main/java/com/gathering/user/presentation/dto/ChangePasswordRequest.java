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
 * 일반 사용자: 현재 비밀번호 확인 후 새 비밀번호로 변경
 * 소셜 로그인 사용자: 현재 비밀번호 없이 새 비밀번호 설정 가능
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangePasswordRequest {

	/**
	 * 현재 비밀번호 (AES 암호화)
	 * 일반 회원가입 사용자: 필수 (본인 인증)
	 * 소셜 로그인 사용자: 선택 (null 또는 빈 값 가능)
	 */
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