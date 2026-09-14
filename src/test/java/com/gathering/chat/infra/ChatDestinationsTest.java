package com.gathering.chat.infra;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatDestinationsTest {

	@Test
	@DisplayName("채팅방 토픽은 /topic/rooms/{roomTsid} 이고 거기서 TSID 를 되돌려 받을 수 있다")
	void roundTrip() {
		assertThat(ChatDestinations.room("01HQROOM000001")).isEqualTo("/topic/rooms/01HQROOM000001");
		assertThat(ChatDestinations.roomTsidOf("/topic/rooms/01HQROOM000001")).contains("01HQROOM000001");
	}

	@Test
	@DisplayName("규약에 맞지 않는 목적지는 채팅방으로 해석하지 않는다")
	void rejectsOtherDestinations() {
		assertThat(ChatDestinations.roomTsidOf(null)).isEmpty();
		assertThat(ChatDestinations.roomTsidOf("/topic/other")).isEmpty();
		assertThat(ChatDestinations.roomTsidOf("/topic/rooms/")).isEmpty();
		assertThat(ChatDestinations.roomTsidOf("/topic/rooms/01HQROOM000001/extra")).isEmpty();
		assertThat(ChatDestinations.roomTsidOf("/queue/rooms/01HQROOM000001")).isEmpty();
	}
}
