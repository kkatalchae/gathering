package com.gathering.chat.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ChatRoomEntity 도메인 로직 테스트
 */
class ChatRoomEntityTest {

	@Test
	@DisplayName("모임 채팅방은 타입이 GATHERING 이고 주체가 모임 TSID 다")
	void forGathering() {
		// when
		ChatRoomEntity room = ChatRoomEntity.forGathering("01HQGATHERING1");

		// then
		assertThat(room.getRoomType()).isEqualTo(ChatRoomType.GATHERING);
		assertThat(room.getGatheringTsid()).isEqualTo("01HQGATHERING1");
		assertThat(room.getScheduleTsid()).isNull();
		assertThat(room.getOwnerTsid()).isEqualTo("01HQGATHERING1");
	}

	@Test
	@DisplayName("일정 채팅방은 타입이 SCHEDULE 이고 주체가 일정 TSID 다")
	void forSchedule() {
		// when
		ChatRoomEntity room = ChatRoomEntity.forSchedule("01HQSCHEDULE01");

		// then
		assertThat(room.getRoomType()).isEqualTo(ChatRoomType.SCHEDULE);
		assertThat(room.getScheduleTsid()).isEqualTo("01HQSCHEDULE01");
		assertThat(room.getGatheringTsid()).isNull();
		assertThat(room.getOwnerTsid()).isEqualTo("01HQSCHEDULE01");
	}
}
