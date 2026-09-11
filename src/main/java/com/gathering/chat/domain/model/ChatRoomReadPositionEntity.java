package com.gathering.chat.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 읽음 위치 (Cassandra: chat_room_read_positions_by_user)
 * 사용자 × 방당 하나이며 "이 사용자가 이 방을 여기까지 읽었다" 는 워터마크다. 멤버십 정보가 아니다 — docs/adr/0004
 * 메시지를 참조하지 않는다: TSID 가 시간순이라 그 메시지가 없어져도 "이 시점 이후" 라는 의미는 유지된다
 */
@Table("chat_room_read_positions_by_user")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_ = @PersistenceCreator)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomReadPositionEntity {

	@PrimaryKey
	private ChatRoomReadPositionKey key;

	@Column("last_read_message_tsid")
	private String lastReadMessageTsid;

	@Column("updated_at")
	private Instant updatedAt;

	public static ChatRoomReadPositionEntity of(String userTsid, String roomTsid, String lastReadMessageTsid) {
		return new ChatRoomReadPositionEntity(
			new ChatRoomReadPositionKey(userTsid, roomTsid), lastReadMessageTsid, Instant.now());
	}

	/**
	 * 읽음 위치를 앞으로만 옮긴다
	 * TSID 는 시간순 문자열이라 문자열 비교로 앞뒤를 판단할 수 있다. 뒤로 가는 요청은 무시한다
	 * Cassandra 엔티티는 JPA 와 달리 변경 감지가 없으므로, 호출 후 반드시 repository.save() 로 저장해야 한다
	 *
	 * @return 실제로 옮겨졌으면 true (호출 측은 이 값이 true 일 때만 save 하면 된다)
	 */
	public boolean advanceTo(String messageTsid) {
		if (messageTsid.compareTo(lastReadMessageTsid) <= 0) {
			return false;
		}
		this.lastReadMessageTsid = messageTsid;
		this.updatedAt = Instant.now();
		return true;
	}

	public String getUserTsid() {
		return key.getUserTsid();
	}

	public String getRoomTsid() {
		return key.getRoomTsid();
	}
}
