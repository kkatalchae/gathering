package com.gathering.user.presentation.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 정보 수정 요청 DTO
 * PATCH /users/me API에서 사용
 * 부분 업데이트를 지원하므로 모든 필드가 nullable
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateMyInfoRequest {

	/**
	 * 닉네임 (선택)
	 * null이면 변경하지 않음
	 */
	private String nickname;

	/**
	 * 이름 (선택)
	 * null이면 변경하지 않음
	 * 빈 문자열은 허용하지 않음
	 */
	private String name;

	/**
	 * 전화번호 (선택)
	 * null이면 변경하지 않음
	 * 10-11자리 숫자만 허용
	 */
	@Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 10-11자리 숫자만 입력해주세요")
	private String phoneNumber;
}