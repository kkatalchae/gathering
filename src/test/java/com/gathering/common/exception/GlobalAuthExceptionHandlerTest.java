package com.gathering.common.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * GlobalAuthExceptionHandler 테스트 — 비관적 락 실패 응답 번역
 */
class GlobalAuthExceptionHandlerTest {

	private final GlobalAuthExceptionHandler handler = new GlobalAuthExceptionHandler();

	@Test
	@DisplayName("데드락 패자로 롤백된 요청은 500 이 아니라 409 로 응답한다")
	void deadlockLoserIsTranslatedToConflict() {
		// given
		DeadlockLoserDataAccessException exception =
			new DeadlockLoserDataAccessException("Deadlock found when trying to get lock", null);

		// when
		ResponseEntity<ErrorResponse> response = handler.handlePessimisticLockingFailure(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.CONCURRENT_REQUEST_CONFLICT.getMessage());
	}

	@Test
	@DisplayName("락 대기 타임아웃도 같은 409 로 응답한다")
	void lockWaitTimeoutIsTranslatedToConflict() {
		// given
		CannotAcquireLockException exception =
			new CannotAcquireLockException("Lock wait timeout exceeded", null);

		// when
		ResponseEntity<ErrorResponse> response = handler.handlePessimisticLockingFailure(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}
}
