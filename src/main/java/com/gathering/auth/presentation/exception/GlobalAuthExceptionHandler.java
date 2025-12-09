package com.gathering.auth.presentation.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.auth.presentation.dto.ErrorResponse;

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
}