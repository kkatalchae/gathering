package com.gathering.chat.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.cassandra.repository.CassandraRepository;

import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;
import com.gathering.chat.domain.model.ChatRoomReadPositionKey;

/**
 * 채팅방 읽음 위치 (Cassandra) — 사용자 파티션 하나에 그 사용자의 모든 방 읽음 위치가 모여 있다
 */
public interface ChatRoomReadPositionRepository
	extends CassandraRepository<ChatRoomReadPositionEntity, ChatRoomReadPositionKey> {

	Optional<ChatRoomReadPositionEntity> findByKeyUserTsidAndKeyRoomTsid(String userTsid, String roomTsid);

	List<ChatRoomReadPositionEntity> findByKeyUserTsid(String userTsid);
}
