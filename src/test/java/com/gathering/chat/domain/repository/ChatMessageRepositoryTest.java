package com.gathering.chat.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.data.domain.Limit;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;

/**
 * Cassandra 리포지토리 테스트 — 실제 Cassandra(Testcontainers) 위에서 파티션 키 조회와 클러스터링 정렬을 검증한다
 * 파생 쿼리가 CQL 로 정확히 번역되는지(ALLOW FILTERING 없이 실행되는지)는 여기서만 잡을 수 있다
 */
@DataCassandraTest
class ChatMessageRepositoryTest {

	private static final String ROOM_TSID = "01HQROOM000001";
	private static final String USER_TSID = "01HQUSER000001";

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomReadPositionRepository readPositionRepository;

	@AfterEach
	void tearDown() {
		chatMessageRepository.deleteByKeyRoomTsidAndKeyBucket(ROOM_TSID, currentBucket());
		readPositionRepository.deleteAll();
	}

	@Test
	@DisplayName("버킷 안의 메시지를 최신순으로 N건 조회한다")
	void findLatestInBucket() {
		// given
		ChatMessageEntity first = save("첫 번째");
		ChatMessageEntity second = save("두 번째");
		ChatMessageEntity third = save("세 번째");

		// when
		List<ChatMessageEntity> latestTwo = chatMessageRepository.findByKeyRoomTsidAndKeyBucket(
			ROOM_TSID, currentBucket(), Limit.of(2));

		// then
		assertThat(latestTwo).extracting(ChatMessageEntity::getMessageTsid)
			.containsExactly(third.getMessageTsid(), second.getMessageTsid())
			.doesNotContain(first.getMessageTsid());
	}

	@Test
	@DisplayName("커서보다 오래된 메시지를 최신순으로 이어서 조회한다")
	void findOlderThanCursor() {
		// given
		ChatMessageEntity first = save("첫 번째");
		ChatMessageEntity second = save("두 번째");
		ChatMessageEntity third = save("세 번째");

		// when
		List<ChatMessageEntity> olderThanThird = chatMessageRepository
			.findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidLessThan(
				ROOM_TSID, currentBucket(), third.getMessageTsid(), Limit.of(10));

		// then
		assertThat(olderThanThird).extracting(ChatMessageEntity::getMessageTsid)
			.containsExactly(second.getMessageTsid(), first.getMessageTsid());
	}

	@Test
	@DisplayName("커서보다 새로운 메시지를 오래된 순으로 조회한다 (재접속 보충)")
	void findNewerThanCursorAscending() {
		// given
		ChatMessageEntity first = save("첫 번째");
		ChatMessageEntity second = save("두 번째");
		ChatMessageEntity third = save("세 번째");

		// when
		List<ChatMessageEntity> newerThanFirst = chatMessageRepository
			.findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThanOrderByKeyMessageTsidAsc(
				ROOM_TSID, currentBucket(), first.getMessageTsid(), Limit.of(10));

		// then
		assertThat(newerThanFirst).extracting(ChatMessageEntity::getMessageTsid)
			.containsExactly(second.getMessageTsid(), third.getMessageTsid());
	}

	@Test
	@DisplayName("커서 이후 메시지 수를 센다 (안 읽은 개수)")
	void countNewerThanCursor() {
		// given
		ChatMessageEntity first = save("첫 번째");
		save("두 번째");
		save("세 번째");

		// when
		long unread = chatMessageRepository.countByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
			ROOM_TSID, currentBucket(), first.getMessageTsid());

		// then
		assertThat(unread).isEqualTo(2L);
	}

	@Test
	@DisplayName("다른 방의 메시지는 같은 버킷이어도 조회되지 않는다")
	void partitionIsolation() {
		// given
		save("우리 방");
		ChatMessageEntity other = chatMessageRepository.save(
			ChatMessageEntity.text("01HQROOM000002", USER_TSID, "다른 방"));

		// when
		List<ChatMessageEntity> ours = chatMessageRepository.findByKeyRoomTsidAndKeyBucket(
			ROOM_TSID, currentBucket(), Limit.of(10));

		// then
		assertThat(ours).hasSize(1);
		assertThat(ours.getFirst().getContent()).isEqualTo("우리 방");
		chatMessageRepository.delete(other);
	}

	@Test
	@DisplayName("읽음 위치는 사용자 파티션에 방별로 저장되고 같은 키로 덮어써진다")
	void readPositionUpsert() {
		// given
		ChatMessageEntity first = save("첫 번째");
		ChatMessageEntity second = save("두 번째");
		readPositionRepository.save(ChatRoomReadPositionEntity.of(USER_TSID, ROOM_TSID, first.getMessageTsid()));

		// when: 같은 (user, room) 키로 다시 저장하면 upsert
		readPositionRepository.save(ChatRoomReadPositionEntity.of(USER_TSID, ROOM_TSID, second.getMessageTsid()));

		// then
		assertThat(readPositionRepository.findByKeyUserTsidAndKeyRoomTsid(USER_TSID, ROOM_TSID))
			.isPresent()
			.get()
			.extracting(ChatRoomReadPositionEntity::getLastReadMessageTsid)
			.isEqualTo(second.getMessageTsid());
		assertThat(readPositionRepository.findByKeyUserTsid(USER_TSID)).hasSize(1);
	}

	private ChatMessageEntity save(String content) {
		return chatMessageRepository.save(ChatMessageEntity.text(ROOM_TSID, USER_TSID, content));
	}

	private String currentBucket() {
		return ChatMessageBucket.of(Instant.now()).getValue();
	}
}
