package com.gathering.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
	AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증 처리 중 오류가 발생했습니다"),

	// Access Token Errors
	ACCESS_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "액세스 토큰이 필요합니다"),
	ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다. 토큰을 갱신해주세요"),
	ACCESS_TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "액세스 토큰 형식이 올바르지 않습니다"),

	// Refresh Token Errors
	REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 필요합니다"),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요"),
	REFRESH_TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "리프레시 토큰 형식이 올바르지 않습니다"),
	REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 취소되었습니다. 다시 로그인해주세요"),

	// 사용자 관련 에러 (404 Not Found)
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	USER_DELETED(HttpStatus.NOT_FOUND, "삭제된 사용자입니다."),

	// 유효성 검증 에러 (400 Bad Request)
	INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바른 형식의 이메일이 아닙니다."),
	INVALID_PHONE_NUMBER_FORMAT(HttpStatus.BAD_REQUEST, "올바른 형식의 전화번호가 아닙니다."),
	INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 최소 8자 이상이며, 숫자와 특수문자(!@#$%^&*)를 포함해야 합니다."),
	INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
	NAME_BLANK(HttpStatus.BAD_REQUEST, "이름은 비어있을 수 없습니다."),

	// 중복 에러 (409 Conflict)
	EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 기존 계정으로 로그인 후 설정에서 소셜 계정을 연동해주세요."),

	// OAuth 관련 에러
	OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
	OAUTH_DIFFERENT_ACCOUNT(
		HttpStatus.CONFLICT,
		"해당 이메일로 가입된 다른 계정이 있습니다. 기존 계정으로 로그인 후 소셜 계정 연동을 진행해주세요."
	),
	OAUTH_PROVIDER_ALREADY_USED(HttpStatus.CONFLICT, "다른 사용자가 이미 사용 중인 소셜 계정입니다."),
	OAUTH_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "연동되지 않은 소셜 계정입니다."),
	CANNOT_UNLINK_LAST_LOGIN_METHOD(
		HttpStatus.BAD_REQUEST,
		"마지막 로그인 수단입니다. 비밀번호를 설정하거나 다른 소셜 계정을 연동한 후 해제할 수 있습니다."
	),

	// Gathering 관련 에러 (400 Bad Request)
	GATHERING_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "모임 이름은 필수입니다"),
	GATHERING_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "모임 이름은 25자를 초과할 수 없습니다"),
	GATHERING_DESCRIPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "모임 설명은 1000자를 초과할 수 없습니다"),

	// Region 관련 에러 (404 Not Found)
	REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다"),

	// Gathering 조회 관련 에러 (404 Not Found)
	GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "모임을 찾을 수 없습니다"),

	// Gathering 권한 관련 에러 (403 Forbidden)
	GATHERING_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "모임을 수정할 권한이 없습니다"),
	GATHERING_DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "모임을 삭제할 권한이 없습니다"),

	// Pagination 관련 에러 (400 Bad Request)
	INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "페이지 크기는 1~100 사이여야 합니다");

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
