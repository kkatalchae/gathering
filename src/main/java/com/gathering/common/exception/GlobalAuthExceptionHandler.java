package com.gathering.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 인증 관련 글로벌 예외 처리
 * Spring Security 표준 방식에 따라 @RestControllerAdvice로 중앙 집중식 예외 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalAuthExceptionHandler {

	/**
	 * 인증 실패 예외 처리
	 * - UsernameNotFoundException: 존재하지 않는 사용자
	 * - BadCredentialsException: 잘못된 비밀번호
	 */
	@ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
	public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception e) {
		log.warn("인증 실패: {}", e.getMessage());
		return ErrorCode.INVALID_CREDENTIALS.toResponseEntity();
	}

	/**
	 * 비즈니스 로직 예외 처리
	 * - 토큰 관련 에러 (ACCESS_TOKEN_EXPIRED, REFRESH_TOKEN_EXPIRED 등)
	 * - 사용자 조회 실패
	 * - 삭제/정지된 사용자 접근
	 * - 기타 비즈니스 규칙 위반
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		log.warn("비즈니스 예외 발생: {} - {}", e.getErrorCode().name(), e.getMessage());
		return e.getErrorCode().toResponseEntity();
	}

	/**
	 * 유효성 검증 실패 예외 처리
	 * @Valid 어노테이션으로 요청 바디 검증 실패 시 발생
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		// 첫 번째 필드 에러만 처리
		FieldError fieldError = e.getBindingResult().getFieldError();

		if (fieldError == null) {
			log.warn("유효성 검증 실패: 필드 에러 없음");
			return ErrorCode.INVALID_EMAIL_FORMAT.toResponseEntity();
		}

		String fieldName = fieldError.getField();
		log.warn("유효성 검증 실패: {} - {}", fieldName, fieldError.getDefaultMessage());

		// 필드 이름으로 ErrorCode 결정
		ErrorCode errorCode = getErrorCodeByField(fieldName);
		return errorCode.toResponseEntity();
	}

	/**
	 * 필드 이름으로 ErrorCode 매핑
	 */
	private ErrorCode getErrorCodeByField(String fieldName) {
		return switch (fieldName) {
			case "email" -> ErrorCode.INVALID_EMAIL_FORMAT;
			case "password" -> ErrorCode.INVALID_PASSWORD_FORMAT;
			case "name" -> ErrorCode.NAME_BLANK;
			case "phoneNumber" -> ErrorCode.INVALID_PHONE_NUMBER_FORMAT;
			default -> ErrorCode.INVALID_EMAIL_FORMAT;
		};
	}
}
