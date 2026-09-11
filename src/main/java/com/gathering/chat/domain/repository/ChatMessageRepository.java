package com.gathering.chat.domain.repository;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Limit;

import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatMessageKey;

/**
 * 채팅 메시지 (Cassandra)
 * 모든 조회는 파티션 키 (room_tsid, bucket) 를 반드시 지정한다. ALLOW FILTERING 은 쓰지 않는다 — docs/adr/0002
 * 버킷을 넘나드는 페이징(현재 달에서 부족하면 이전 달)은 서비스가 ChatMessageBucket.previous() 로 조율한다
 * @Transactional(JPA) 의 영향을 받지 않고 즉시 실행된다 — MySQL 쓰기가 있는 트랜잭션 안에서 호출하지 않는다 (ADR-0002)
 */
public interface ChatMessageRepository extends CassandraRepository<ChatMessageEntity, ChatMessageKey> {

	/** 버킷 안의 최신 메시지부터 N건 (첫 페이지) */
	List<ChatMessageEntity> findByKeyRoomTsidAndKeyBucket(String roomTsid, String bucket, Limit limit);

	/** 커서보다 오래된 메시지부터 N건 (이전 페이지, 최신순) */
	List<ChatMessageEntity> findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidLessThan(
		String roomTsid, String bucket, String messageTsid, Limit limit);

	/** 커서보다 새로운 메시지 (재접속 시 놓친 메시지 보충, 오래된 것부터) */
	List<ChatMessageEntity> findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThanOrderByKeyMessageTsidAsc(
		String roomTsid, String bucket, String messageTsid, Limit limit);

	/** 커서 이후 메시지 수 (안 읽은 개수, 호출 측이 Limit 로 상한을 둔다) */
	long countByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(String roomTsid, String bucket, String messageTsid);

	/** 파티션 통째로 삭제 (방 삭제 시 버킷마다 호출) */
	void deleteByKeyRoomTsidAndKeyBucket(String roomTsid, String bucket);
}
