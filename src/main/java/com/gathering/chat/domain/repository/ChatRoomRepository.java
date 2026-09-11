package com.gathering.chat.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.chat.domain.model.ChatRoomEntity;

/**
 * 채팅방 메타데이터 (MySQL) — 모임/일정과의 연결과 멤버십 파생의 출발점
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, String> {

	Optional<ChatRoomEntity> findByGatheringTsid(String gatheringTsid);

	Optional<ChatRoomEntity> findByScheduleTsid(String scheduleTsid);
}
