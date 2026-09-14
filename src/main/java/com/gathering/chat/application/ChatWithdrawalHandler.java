package com.gathering.chat.application;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.user.domain.policy.UserWithdrawalHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 시 채팅 도메인 정리
 * - 읽음 위치(Cassandra, 사용자 파티션)를 삭제한다
 * - 메시지의 sender_tsid 는 지우지 않는다: 메시지는 방의 기록이며 익명화된 사용자 row 가 "탈퇴한 사용자" 로 남는다
 * Cassandra 쓰기는 JPA 트랜잭션 롤백에 영향받지 않으므로, MySQL 정리가 커밋된 뒤에만 실행한다 (docs/adr/0002)
 * 실패해도 탈퇴는 이미 완료된 상태이며 남는 것은 접근 경로 없는 워터마크뿐이라 경고만 남긴다
 */
@Slf4j
@Component
@Order(ChatWithdrawalHandler.ORDER)
@RequiredArgsConstructor
public class ChatWithdrawalHandler implements UserWithdrawalHandler {

	public static final int ORDER = 300;

	private final ChatRoomReadPositionRepository readPositionRepository;

	@Override
	public void cleanUp(String userTsid) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteReadPositions(userTsid);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deleteReadPositions(userTsid);
			}
		});
	}

	private void deleteReadPositions(String userTsid) {
		try {
			readPositionRepository.deleteByKeyUserTsid(userTsid);
		} catch (RuntimeException e) {
			log.warn("탈퇴한 사용자의 채팅 읽음 위치 삭제 실패 (무해한 잔여 데이터): userTsid={}, cause={}",
				userTsid, e.getMessage());
		}
	}
}
