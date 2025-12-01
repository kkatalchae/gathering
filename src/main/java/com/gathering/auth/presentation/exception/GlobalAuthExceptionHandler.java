package com.gathering.auth.presentation.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
	public ResponseEntity<Void> handleAuthenticationException(Exception e) {
		log.warn("인증 실패: {}", e.getMessage());
		return ResponseEntity.status(401).build();
	}
}