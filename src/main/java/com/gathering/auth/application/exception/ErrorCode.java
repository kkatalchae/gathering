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
	USER_BANNED(HttpStatus.NOT_FOUND, "사용이 제한된 사용자입니다."),

	// 유효성 검증 에러 (400 Bad Request)
	INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바른 형식의 이메일이 아닙니다."),
	INVALID_PHONE_NUMBER_FORMAT(HttpStatus.BAD_REQUEST, "올바른 형식의 전화번호가 아닙니다."),
	INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 최소 8자 이상이며, 숫자와 특수문자(!@#$%^&*)를 포함해야 합니다."),
	INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
	NAME_BLANK(HttpStatus.BAD_REQUEST, "이름은 비어있을 수 없습니다."),

	// 중복 에러 (409 Conflict)
	EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
	PHONE_NUMBER_DUPLICATE(HttpStatus.CONFLICT, "이미 사용중인 전화번호입니다.");

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