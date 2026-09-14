package com.gathering.chat.application;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;

/**
 * ChatWithdrawalHandler 테스트 — Cassandra 삭제가 JPA 커밋 뒤로 미뤄지는지 검증한다
 */
@ExtendWith(MockitoExtension.class)
class ChatWithdrawalHandlerTest {

	private static final String USER_TSID = "01HQUSER000001";

	@Mock
	private ChatRoomReadPositionRepository readPositionRepository;

	@InjectMocks
	private ChatWithdrawalHandler handler;

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("트랜잭션 안에서는 즉시 지우지 않고 커밋 후에 읽음 위치를 삭제한다")
	void deletesAfterCommitInsideTransaction() {
		// given: 트랜잭션 동기화가 활성화된 상태를 흉내 낸다
		TransactionSynchronizationManager.initSynchronization();

		// when
		handler.cleanUp(USER_TSID);

		// then: 아직 지우지 않았다
		then(readPositionRepository).should(never()).deleteByKeyUserTsid(any());

		// when: 커밋이 일어나면
		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

		// then
		then(readPositionRepository).should().deleteByKeyUserTsid(USER_TSID);
	}

	@Test
	@DisplayName("트랜잭션 밖에서는 바로 삭제한다")
	void deletesImmediatelyOutsideTransaction() {
		// when
		handler.cleanUp(USER_TSID);

		// then
		then(readPositionRepository).should().deleteByKeyUserTsid(USER_TSID);
	}

	@Test
	@DisplayName("Cassandra 삭제가 실패해도 예외를 밖으로 던지지 않는다 (탈퇴는 이미 완료)")
	void swallowsCassandraFailure() {
		// given
		willThrow(new RuntimeException("cassandra down")).given(readPositionRepository).deleteByKeyUserTsid(USER_TSID);

		// when & then: 예외 없이 끝난다
		handler.cleanUp(USER_TSID);
	}
}
