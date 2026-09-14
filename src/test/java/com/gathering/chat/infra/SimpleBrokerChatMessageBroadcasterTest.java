package com.gathering.chat.infra;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.gathering.chat.application.ChatMessageSentEvent;
import com.gathering.chat.presentation.dto.ChatMessageResponse;

@ExtendWith(MockitoExtension.class)
class SimpleBrokerChatMessageBroadcasterTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private SimpleBrokerChatMessageBroadcaster broadcaster;

	@Test
	@DisplayName("방 토픽으로 응답 DTO 를 그대로 보낸다 — REST 조회와 같은 형태")
	void sendsToRoomTopic() {
		ChatMessageResponse message = ChatMessageResponse.builder().messageTsid("0HZZZZZZZZZZ1").content("hi").build();

		broadcaster.broadcast(new ChatMessageSentEvent("01HQROOM000001", message));

		then(messagingTemplate).should().convertAndSend("/topic/rooms/01HQROOM000001", message);
	}
}
