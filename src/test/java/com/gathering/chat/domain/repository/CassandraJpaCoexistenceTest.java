package com.gathering.chat.domain.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;

/**
 * JPA 트랜잭션과 Cassandra 쓰기의 관계를 고정하는 테스트 — docs/adr/0002 "JPA 와 병행할 때 지켜야 할 것"
 *
 * Cassandra 에는 트랜잭션이 없고 Spring 의 @Transactional(JPA 트랜잭션 매니저)은 Cassandra 리포지토리에 아무 영향도 주지 않는다.
 * 따라서 JPA 트랜잭션 안에서 Cassandra 에 쓴 뒤 롤백되면, MySQL 변경은 사라지지만 Cassandra 데이터는 남는다.
 * 이 테스트는 그 사실을 실제로 재현해, "MySQL 쓰기가 있는 트랜잭션 안에서는 Cassandra 에 쓰지 않는다" 는 규칙의 근거를 남긴다.
 */
@SpringBootTest
class CassandraJpaCoexistenceTest {

	private static final String ROOM_TSID = "01HQROOMCOEXIST";
	private static final String USER_TSID = "01HQUSERCOEXIST";

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomReadPositionRepository readPositionRepository;

	@AfterEach
	void tearDown() {
		chatMessageRepository.deleteByKeyRoomTsidAndKeyBucket(ROOM_TSID, ChatMessageBucket.of(Instant.now()).getValue());
		readPositionRepository.findByKeyUserTsidAndKeyRoomTsid(USER_TSID, ROOM_TSID)
			.ifPresent(readPositionRepository::delete);
	}

	@Test
	@DisplayName("JPA 트랜잭션이 롤백되어도 그 안에서 저장한 Cassandra 메시지는 남는다")
	void cassandraWriteSurvivesJpaRollback() {
		// given
		ChatMessageEntity message = ChatMessageEntity.text(ROOM_TSID, USER_TSID, "롤백 실험");

		// when: JPA 트랜잭션 안에서 Cassandra 에 쓰고 예외로 롤백
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			chatMessageRepository.save(message);
			throw new IllegalStateException("MySQL 쪽 작업이 실패했다고 가정");
		})).isInstanceOf(IllegalStateException.class);

		// then: Cassandra 에는 그대로 남아 있다
		assertThat(chatMessageRepository.findById(message.getKey())).isPresent();
	}

	@Test
	@DisplayName("Cassandra 엔티티는 JPA 와 달리 변경 감지가 없어 수정 후 save 를 호출해야 반영된다")
	void cassandraEntityRequiresExplicitSave() {
		// given
		readPositionRepository.save(ChatRoomReadPositionEntity.of(USER_TSID, ROOM_TSID, "0HZZZZZZZZZZ1"));
		ChatRoomReadPositionEntity loaded = readPositionRepository
			.findByKeyUserTsidAndKeyRoomTsid(USER_TSID, ROOM_TSID).orElseThrow();

		// when: 트랜잭션 안에서 엔티티만 바꾸고 save 를 하지 않으면
		transactionTemplate.executeWithoutResult(status -> loaded.advanceTo("0HZZZZZZZZZZ9"));

		// then: 저장소에는 반영되지 않는다 (JPA 라면 dirty checking 으로 반영됐을 것)
		assertThat(readPositionRepository.findByKeyUserTsidAndKeyRoomTsid(USER_TSID, ROOM_TSID))
			.get().extracting(ChatRoomReadPositionEntity::getLastReadMessageTsid).isEqualTo("0HZZZZZZZZZZ1");

		// when: 명시적으로 save 하면
		readPositionRepository.save(loaded);

		// then: 반영된다
		assertThat(readPositionRepository.findByKeyUserTsidAndKeyRoomTsid(USER_TSID, ROOM_TSID))
			.get().extracting(ChatRoomReadPositionEntity::getLastReadMessageTsid).isEqualTo("0HZZZZZZZZZZ9");
	}
}
