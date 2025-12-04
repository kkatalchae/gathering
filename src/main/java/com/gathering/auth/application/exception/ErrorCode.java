package com.gathering.auth.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.gathering.auth.presentation.dto.ErrorResponse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 Enum
 * HTTP 상태 코드와 메시지를 함께 관리
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 인증 관련 에러 (401 Unauthorized)
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다"),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
	TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "토큰이 일치하지 않습니다. 다시 로그인해주세요"),

	// 사용자 관련 에러 (404 Not Found)
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	USER_DELETED(HttpStatus.NOT_FOUND, "삭제된 사용자입니다."),
	USER_BANNED(HttpStatus.NOT_FOUND, "사용이 제한된 사용자입니다.");

	private final HttpStatus httpStatus;
	private final String message;

	/**
	 * ErrorCode로부터 ResponseEntity 생성
	 * @return HTTP 응답
	 */
	public ResponseEntity<ErrorResponse> toResponseEntity() {
		return ResponseEntity
			.status(this.httpStatus)
			.body(ErrorResponse.from(this));
	}
}