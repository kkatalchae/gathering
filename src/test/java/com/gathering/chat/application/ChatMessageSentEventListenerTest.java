package com.gathering.chat.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.chat.presentation.dto.ChatMessageResponse;

@ExtendWith(MockitoExtension.class)
class ChatMessageSentEventListenerTest {

	@Mock
	private ChatMessageBroadcaster broadcaster;

	@InjectMocks
	private ChatMessageSentEventListener listener;

	@Test
	@DisplayName("이벤트를 받으면 브로드캐스터에 그대로 넘긴다")
	void delegatesToBroadcaster() {
		ChatMessageSentEvent event = new ChatMessageSentEvent("01HQROOM000001",
			ChatMessageResponse.builder().messageTsid("0HZZZZZZZZZZ1").content("hi").build());

		listener.onMessageSent(event);

		then(broadcaster).should().broadcast(event);
	}

	@Test
	@DisplayName("브로드캐스트가 실패해도 예외를 밖으로 던지지 않는다 (저장은 이미 완료)")
	void swallowsBroadcastFailure() {
		ChatMessageSentEvent event = new ChatMessageSentEvent("01HQROOM000001",
			ChatMessageResponse.builder().messageTsid("0HZZZZZZZZZZ1").content("hi").build());
		willThrow(new RuntimeException("broker down")).given(broadcaster).broadcast(event);

		assertThatCode(() -> listener.onMessageSent(event)).doesNotThrowAnyException();
	}
}
