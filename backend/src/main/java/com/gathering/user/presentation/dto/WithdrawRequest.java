package com.gathering.user.presentation.dto;

import com.gathering.common.annotation.AesEncrypted;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 탈퇴 요청 DTO
 * DELETE /users/me API에서 사용
 * 비밀번호 확인을 통한 본인 인증 후 회원 탈퇴
 * 소셜 로그인 사용자의 경우 비밀번호 없이 탈퇴 가능
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawRequest {

	/**
	 * 비밀번호 (AES 암호화)
	 * 일반 회원가입 사용자: 본인 확인을 위한 비밀번호 필수
	 * 소셜 로그인 사용자: null 또는 빈 문자열 가능
	 */
	@AesEncrypted
	private String password;
}