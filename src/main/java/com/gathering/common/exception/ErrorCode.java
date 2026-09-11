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
	GATHERING_OWNER_PERMISSION_NEEDED(HttpStatus.FORBIDDEN, "모임의 오너 권한이 필요합니다"),

	// Gathering 참여자 관련 에러 (404 Not Found)
	PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모임에서 사용자를 찾을 수 없습니다"),

	// Gathering 역할 변경 관련 에러 (400 Bad Request)
	CANNOT_CHANGE_OWN_ROLE(HttpStatus.BAD_REQUEST, "자신의 역할은 변경할 수 없습니다"),

	// Gathering 참여/퇴장 관련 에러
	ALREADY_JOINED_GATHERING(HttpStatus.CONFLICT, "이미 참여중인 모임입니다"),
	GATHERING_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "모임 정원이 초과되었습니다"),
	OWNER_CANNOT_LEAVE_GATHERING(HttpStatus.BAD_REQUEST, "오너는 모임을 나갈 수 없습니다. 오너를 양도하거나 모임을 삭제해주세요"),

	// Schedule 유효성 검증 에러 (400 Bad Request)
	SCHEDULE_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "일정 제목은 필수입니다"),
	SCHEDULE_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "일정 제목은 50자를 초과할 수 없습니다"),
	SCHEDULE_DESCRIPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "일정 설명은 1000자를 초과할 수 없습니다"),
	SCHEDULE_START_AT_REQUIRED(HttpStatus.BAD_REQUEST, "일정 시작 시각은 필수입니다"),
	SCHEDULE_END_AT_BEFORE_START_AT(HttpStatus.BAD_REQUEST, "일정 종료 시각은 시작 시각보다 이후여야 합니다"),
	INVALID_SCHEDULE_CAPACITY(HttpStatus.BAD_REQUEST, "일정 정원은 1명 이상이어야 합니다"),
	SCHEDULE_LOCATION_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "일정 장소는 필수입니다"),
	SCHEDULE_LOCATION_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "장소명은 100자를 초과할 수 없습니다"),
	SCHEDULE_LOCATION_ADDRESS_TOO_LONG(HttpStatus.BAD_REQUEST, "장소 주소는 255자를 초과할 수 없습니다"),
	SCHEDULE_CAPACITY_BELOW_PARTICIPANT_COUNT(HttpStatus.BAD_REQUEST, "정원을 현재 참여 인원보다 적게 변경할 수 없습니다"),

	// Schedule 조회 관련 에러 (404 Not Found)
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다"),

	// Schedule 권한 관련 에러 (403 Forbidden)
	SCHEDULE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "일정의 호스트만 수정하거나 삭제할 수 있습니다"),
	NOT_GATHERING_PARTICIPANT(HttpStatus.FORBIDDEN, "모임에 참여한 사용자만 해당 모임의 일정을 열 수 있습니다"),

	// Schedule 참여/취소 관련 에러
	ALREADY_JOINED_SCHEDULE(HttpStatus.CONFLICT, "이미 참여중인 일정입니다"),
	SCHEDULE_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "일정 정원이 가득 찼습니다"),
	SCHEDULE_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작된 일정에는 참여할 수 없습니다"),
	SCHEDULE_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 일정에 참여하고 있지 않습니다"),
	HOST_CANNOT_LEAVE_SCHEDULE(HttpStatus.BAD_REQUEST, "호스트는 일정에서 빠질 수 없습니다. 일정을 삭제해주세요"),

	// 동시성 관련 에러 (409 Conflict) — 데드락 패자 / 락 대기 타임아웃, 재시도하면 성공한다
	CONCURRENT_REQUEST_CONFLICT(HttpStatus.CONFLICT, "동시에 처리 중인 요청이 있습니다. 잠시 후 다시 시도해주세요"),

	// Pagination 관련 에러 (400 Bad Request)
	INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "페이지 크기는 1~100 사이여야 합니다"),
	INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서 형식이 올바르지 않습니다"),

	GATHERING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 모임에 대한 권한이 없습니다."),

	// 파일 업로드 관련 에러 (400 Bad Request)
	FILE_EMPTY(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다."),
	FILE_EXTENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다."),
	FILE_MIME_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
	FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 허용 범위를 초과했습니다."),
	FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
	FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");

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
