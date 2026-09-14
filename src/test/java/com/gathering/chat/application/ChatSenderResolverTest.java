package com.gathering.chat.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.chat.presentation.dto.ChatSenderSummary;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * ChatSenderResolver 테스트
 */
@ExtendWith(MockitoExtension.class)
class ChatSenderResolverTest {

	@Mock
	private UsersRepository usersRepository;

	@InjectMocks
	private ChatSenderResolver senderResolver;

	@Test
	@DisplayName("중복과 null(SYSTEM) 을 걸러 users 를 IN 한 번으로 조회한다")
	void resolveDistinctNonNull() {
		// given
		given(usersRepository.findAllById(List.of("U1", "U2")))
			.willReturn(List.of(user("U1", "하나"), user("U2", "둘")));

		// when
		Map<String, ChatSenderSummary> senders = senderResolver.resolve(Arrays.asList("U1", null, "U2", "U1"));

		// then
		assertThat(senders).containsOnlyKeys("U1", "U2");
		assertThat(senders.get("U2").getNickname()).isEqualTo("둘");
		then(usersRepository).should(times(1)).findAllById(any());
	}

	@Test
	@DisplayName("발신자가 없으면 조회하지 않는다")
	void resolveEmpty() {
		assertThat(senderResolver.resolve(Arrays.asList((String)null))).isEmpty();
		then(usersRepository).shouldHaveNoInteractions();
	}

	private UsersEntity user(String tsid, String nickname) {
		return UsersEntity.builder().tsid(tsid).email(tsid + "@example.com").name("이름").nickname(nickname).build();
	}
}
