package com.gathering.chat.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 (Cassandra: chat_messages_by_room)
 * 조회 패턴 "방의 최근 N건 / 커서 이후 N건" 에 맞춘 테이블이다 — docs/adr/0002 참고
 * MySQL 이 아니므로 FK 가 없다. 방 존재와 멤버십은 저장 전에 애플리케이션이 검증한다
 */
@Table("chat_messages_by_room")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_ = @PersistenceCreator)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity {

	@PrimaryKey
	private ChatMessageKey key;

	/** SYSTEM 메시지는 보낸 사람이 없으므로 null */
	@Column("sender_tsid")
	private String senderTsid;

	@Column("message_type")
	@CassandraType(type = CassandraType.Name.TEXT)
	private ChatMessageType messageType;

	@Column("content")
	private String content;

	/** TSID 와 중복 정보이며 사람이 읽기 위한 편의 컬럼. 정렬에는 쓰지 않는다 */
	@Column("created_at")
	private Instant createdAt;

	/**
	 * 사용자가 보낸 일반 메시지 생성
	 * TSID 를 지금 시각으로 발급하고 그 시각으로 버킷을 정한다
	 */
	public static ChatMessageEntity text(String roomTsid, String senderTsid, String content) {
		return create(roomTsid, senderTsid, ChatMessageType.TEXT, content);
	}

	/**
	 * 서버가 생성한 안내 메시지 (입장/퇴장 등), 보낸 사람 없음
	 */
	public static ChatMessageEntity system(String roomTsid, String content) {
		return create(roomTsid, null, ChatMessageType.SYSTEM, content);
	}

	private static ChatMessageEntity create(String roomTsid, String senderTsid, ChatMessageType type,
		String content) {
		Tsid tsid = TsidCreator.getTsid();
		Instant createdAt = tsid.getInstant();
		ChatMessageKey key = new ChatMessageKey(roomTsid, ChatMessageBucket.of(createdAt).getValue(), tsid.toString());
		return new ChatMessageEntity(key, senderTsid, type, content, createdAt);
	}

	public String getRoomTsid() {
		return key.getRoomTsid();
	}

	public String getMessageTsid() {
		return key.getMessageTsid();
	}
}
