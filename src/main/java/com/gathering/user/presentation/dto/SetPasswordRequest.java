package com.gathering.user.presentation.dto;

import com.gathering.common.annotation.AesEncrypted;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 최초 설정 요청 DTO
 * 소셜 로그인 사용자가 비밀번호를 설정하거나, 기존 비밀번호를 재설정할 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SetPasswordRequest {

	/**
	 * 새로 설정할 비밀번호 (AES 암호화되어 전송됨)
	 * @AesEncrypted 어노테이션에 의해 자동으로 복호화됨
	 */
	@AesEncrypted
	private String password;
}