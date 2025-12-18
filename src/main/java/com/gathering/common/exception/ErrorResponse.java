package com.gathering.common.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에러 응답 DTO
 * ErrorCode를 통해서만 생성 가능 (타입 안전성 보장)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 직렬화용
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 외부 생성 차단
public class ErrorResponse {

	/**
	 * 에러 코드 (예: INVALID_TOKEN, EXPIRED_TOKEN)
	 */
	private String code;

	/**
	 * 사용자에게 표시할 에러 메시지
	 */
	private String message;

	/**
	 * ErrorCode로부터 ErrorResponse 생성
	 * @param errorCode 에러 코드
	 * @return ErrorResponse
	 */
	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}
}
