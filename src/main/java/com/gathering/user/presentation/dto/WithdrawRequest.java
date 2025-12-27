package com.gathering.user.presentation.dto;

import com.gathering.common.annotation.AesEncrypted;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 탈퇴 요청 DTO
 * DELETE /users/me API에서 사용
 * 비밀번호 확인을 통한 본인 인증 후 회원 탈퇴
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawRequest {

	/**
	 * 비밀번호 (AES 암호화)
	 * 본인 확인을 위한 비밀번호 입력 필수
	 */
	@NotNull(message = "비밀번호를 입력해주세요")
	@AesEncrypted
	private String password;
}