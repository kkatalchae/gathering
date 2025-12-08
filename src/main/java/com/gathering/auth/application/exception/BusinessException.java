package com.gathering.auth.application.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 * ErrorCode를 포함하여 일관된 에러 응답 제공
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
}