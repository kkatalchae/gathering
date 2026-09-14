package com.gathering.user.domain.policy;

import com.gathering.common.exception.BusinessException;

/**
 * 회원 탈퇴 시 자기 도메인의 연관 데이터를 책임지는 확장 지점
 *
 * 사용자를 참조하는 도메인(모임, 일정, 채팅 …)은 이 인터페이스를 구현해 빈으로 등록하기만 하면 된다.
 * UserWithdrawalService 가 모든 구현체를 모아 두 단계로 호출한다:
 *   1. validateWithdrawable — 전부 통과해야 다음 단계로 간다. 하나라도 거부하면 아무것도 지우지 않는다
 *   2. cleanUp — @Order 순서대로 정리한다
 * 실행 순서가 중요한 구현체(예: 일정은 모임보다 먼저)는 @Order 를 지정한다.
 * MySQL 쓰기가 있는 트랜잭션 안에서 호출되므로, Cassandra 정리는 커밋 후로 미룬다 (docs/adr/0002)
 */
public interface UserWithdrawalHandler {

	/**
	 * 이 도메인 관점에서 탈퇴를 막아야 하는 상태인지 검사한다
	 *
	 * @param userTsid 탈퇴하려는 사용자 TSID
	 * @throws BusinessException 탈퇴할 수 없는 상태인 경우 (사용자가 먼저 해결해야 하는 사유를 담는다)
	 */
	default void validateWithdrawable(String userTsid) {
	}

	/**
	 * 이 도메인에서 사용자를 가리키는 데이터를 정리한다
	 * 사용자 row 는 익명화되어 남으므로 "기록" 성격의 참조(지난 일정의 호스트 등)는 지우지 않아도 된다
	 *
	 * @param userTsid 탈퇴하는 사용자 TSID
	 */
	void cleanUp(String userTsid);
}
