package com.gathering.auth.application.exception;

/**
 * 토큰이 저장된 값과 일치하지 않을 때 발생하는 예외
 * - 토큰이 탈취되어 재사용된 경우
 * - 이미 로그아웃된 경우
 * - 저장소에 토큰이 존재하지 않는 경우
 */
public class TokenMismatchException extends RuntimeException {

	public TokenMismatchException(String message) {
		super(message);
	}

	public TokenMismatchException(String message, Throwable cause) {
		super(message, cause);
	}
}