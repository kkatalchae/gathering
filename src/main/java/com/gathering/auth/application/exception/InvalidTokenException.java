package com.gathering.auth.application.exception;

/**
 * 토큰이 유효하지 않을 때 발생하는 예외
 * - JWT 형식 오류
 * - 서명 검증 실패
 * - 지원되지 않는 토큰
 */
public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message) {
		super(message);
	}

	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}