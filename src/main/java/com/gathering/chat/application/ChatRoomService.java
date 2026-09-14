package com.gathering.chat.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅방 생명주기 — 모임/일정과 함께 태어나고 함께 사라진다
 * MySQL row 삭제는 호출 측 트랜잭션 안에서, Cassandra 메시지 파티션 삭제는 커밋 후 best-effort 로 한다 (docs/adr/0002)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional
	public ChatRoomEntity createForGathering(String gatheringTsid) {
		return chatRoomRepository.save(ChatRoomEntity.forGathering(gatheringTsid));
	}

	@Transactional
	public ChatRoomEntity createForSchedule(String scheduleTsid) {
		return chatRoomRepository.save(ChatRoomEntity.forSchedule(scheduleTsid));
	}

	@Transactional(readOnly = true)
	public Optional<ChatRoomEntity> findByGathering(String gatheringTsid) {
		return chatRoomRepository.findByGatheringTsid(gatheringTsid);
	}

	@Transactional(readOnly = true)
	public Optional<ChatRoomEntity> findBySchedule(String scheduleTsid) {
		return chatRoomRepository.findByScheduleTsid(scheduleTsid);
	}

	/**
	 * 모임 채팅방 삭제 — 모임 row 삭제 전에 호출한다 (FK)
	 */
	@Transactional
	public void deleteForGathering(String gatheringTsid) {
		chatRoomRepository.findByGatheringTsid(gatheringTsid).ifPresent(room -> deleteRooms(List.of(room)));
	}

	/**
	 * 일정 채팅방 일괄 삭제 — 일정 row 삭제 전에 호출한다 (FK)
	 */
	@Transactional
	public void deleteForSchedules(List<String> scheduleTsids) {
		if (scheduleTsids.isEmpty()) {
			return;
		}
		deleteRooms(chatRoomRepository.findAllByScheduleTsidIn(scheduleTsids));
	}

	private void deleteRooms(List<ChatRoomEntity> rooms) {
		if (rooms.isEmpty()) {
			return;
		}

		// 커밋 후 지울 파티션을 먼저 계산해 둔다 — 벌크 삭제가 컨텍스트를 비우면 room 엔티티는 detached 가 된다
		List<RoomPartitions> partitions = rooms.stream()
			.map(room -> new RoomPartitions(room.getTsid(), room.getCreatedAt()))
			.toList();
		chatRoomRepository.deleteAllByTsidIn(rooms.stream().map(ChatRoomEntity::getTsid).toList());

		// Cassandra 는 트랜잭션에 참여하지 않으므로 MySQL 커밋이 확정된 뒤에만 지운다
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				partitions.forEach(ChatRoomService.this::deleteMessagePartitions);
			}
		});
	}

	/**
	 * 방 생성 월부터 현재 월까지 버킷을 순회하며 파티션을 지운다
	 * 빈 파티션 삭제는 톰스톤 하나로 무해하다. 실패하면 방이 없어 접근 경로가 없는 고아 파티션이 남을 뿐이라 경고만 남긴다
	 */
	private void deleteMessagePartitions(RoomPartitions target) {
		ChatMessageBucket bucket = ChatMessageBucket.of(Instant.now());
		ChatMessageBucket oldest = ChatMessageBucket.of(target.createdAt());
		try {
			while (!bucket.isBefore(oldest)) {
				chatMessageRepository.deleteByKeyRoomTsidAndKeyBucket(target.roomTsid(), bucket.getValue());
				bucket = bucket.previous();
			}
		} catch (RuntimeException e) {
			log.warn("채팅 메시지 파티션 삭제 실패 (고아 파티션, 무해): roomTsid={}, bucket={}, cause={}",
				target.roomTsid(), bucket.getValue(), e.getMessage());
		}
	}

	private record RoomPartitions(String roomTsid, Instant createdAt) {
	}
}
