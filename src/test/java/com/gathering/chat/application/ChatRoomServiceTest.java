package com.gathering.chat.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;

/**
 * ChatRoomService 테스트 — 삭제 시 MySQL row 는 즉시, Cassandra 파티션은 커밋 후 방 생성 월부터 순회하며 지우는지 검증한다
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@InjectMocks
	private ChatRoomService chatRoomService;

	@BeforeEach
	void setUp() {
		TransactionSynchronizationManager.initSynchronization();
	}

	@AfterEach
	void tearDown() {
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	@DisplayName("모임 채팅방 삭제 시 row 는 트랜잭션 안에서 지우고 메시지 파티션은 커밋 후 생성 월부터 현재 월까지 지운다")
	void deleteForGatheringRemovesRowThenPartitionsAfterCommit() {
		// given: 두 달 전에 만들어진 방
		ChatRoomEntity room = roomCreatedAt("01HQROOM000001", Instant.now().minusSeconds(60L * 60 * 24 * 62));
		given(chatRoomRepository.findByGatheringTsid("01HQGATHERING1")).willReturn(Optional.of(room));

		// when
		chatRoomService.deleteForGathering("01HQGATHERING1");

		// then: MySQL 은 즉시, Cassandra 는 아직
		then(chatRoomRepository).should().deleteAllByTsidIn(List.of("01HQROOM000001"));
		then(chatMessageRepository).should(never()).deleteByKeyRoomTsidAndKeyBucket(any(), any());

		// when: 커밋
		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

		// then: 생성 월 ~ 현재 월 = 3개 버킷 (62일 전이면 두 달 경계를 넘는다)
		then(chatMessageRepository).should(atLeast(2)).deleteByKeyRoomTsidAndKeyBucket(eq("01HQROOM000001"), any());
		then(chatMessageRepository).should(atMost(4)).deleteByKeyRoomTsidAndKeyBucket(eq("01HQROOM000001"), any());
	}

	@Test
	@DisplayName("일정 채팅방 일괄 삭제는 방이 없으면 아무것도 하지 않는다")
	void deleteForSchedulesWithoutRooms() {
		// given
		given(chatRoomRepository.findAllByScheduleTsidIn(List.of("01HQSCHEDULE01"))).willReturn(List.of());

		// when
		chatRoomService.deleteForSchedules(List.of("01HQSCHEDULE01"));

		// then
		then(chatRoomRepository).should(never()).deleteAllByTsidIn(any());
	}

	@Test
	@DisplayName("Cassandra 파티션 삭제가 실패해도 예외를 던지지 않는다 (고아 파티션은 무해)")
	void partitionDeleteFailureIsSwallowed() {
		// given
		ChatRoomEntity room = roomCreatedAt("01HQROOM000001", Instant.now());
		given(chatRoomRepository.findByGatheringTsid("01HQGATHERING1")).willReturn(Optional.of(room));
		willThrow(new RuntimeException("cassandra down"))
			.given(chatMessageRepository).deleteByKeyRoomTsidAndKeyBucket(any(), any());

		// when
		chatRoomService.deleteForGathering("01HQGATHERING1");

		// then: afterCommit 이 예외 없이 끝난다
		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
	}

	private ChatRoomEntity roomCreatedAt(String tsid, Instant createdAt) {
		ChatRoomEntity room = mock(ChatRoomEntity.class);
		given(room.getTsid()).willReturn(tsid);
		given(room.getCreatedAt()).willReturn(createdAt);
		return room;
	}
}
