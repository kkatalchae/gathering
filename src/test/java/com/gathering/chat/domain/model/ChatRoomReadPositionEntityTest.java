package com.gathering.chat.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ChatRoomReadPositionEntity 도메인 로직 테스트
 */
class ChatRoomReadPositionEntityTest {

	@Test
	@DisplayName("읽음 위치는 더 나중 메시지로만 옮겨진다")
	void advanceForward() {
		// given
		ChatRoomReadPositionEntity position =
			ChatRoomReadPositionEntity.of("01HQUSER000001", "01HQROOM000001", "0HZZZZZZZZZZ5");

		// when
		boolean advanced = position.advanceTo("0HZZZZZZZZZZ9");

		// then
		assertThat(advanced).isTrue();
		assertThat(position.getLastReadMessageTsid()).isEqualTo("0HZZZZZZZZZZ9");
	}

	@Test
	@DisplayName("이미 읽은 위치보다 앞선 메시지로는 되돌아가지 않는다")
	void doesNotMoveBackward() {
		// given
		ChatRoomReadPositionEntity position =
			ChatRoomReadPositionEntity.of("01HQUSER000001", "01HQROOM000001", "0HZZZZZZZZZZ9");

		// when
		boolean advanced = position.advanceTo("0HZZZZZZZZZZ5");
		boolean same = position.advanceTo("0HZZZZZZZZZZ9");

		// then
		assertThat(advanced).isFalse();
		assertThat(same).isFalse();
		assertThat(position.getLastReadMessageTsid()).isEqualTo("0HZZZZZZZZZZ9");
	}
}
