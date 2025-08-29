package com.gathering.user.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;

@ExtendWith(MockitoExtension.class)
class UserJoinServiceTest {

	@InjectMocks
	private UserJoinValidateService userJoinValidateService;

	@Mock
	private UsersRepository usersRepository;

	@Nested
	@DisplayName("회원가입")
	class Join {

		// fail test
		@Test
		@DisplayName("이메일이 null 이면 실패")
		void emailIsNull() {
			// given
			UserJoinRequest request = UserJoinRequest.builder()
				.email(null)
				.password("password")
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when
			// then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUsersEntity(request),
				UserJoinValidateService.ERROR_MESSAGE_EMAIL_INVALID_FORMAT);
		}

		@Test
		@DisplayName("올바른 형식의 이메일이 아니면 실패")
		void emailIsNotValidFormat() {
			// given
			UserJoinRequest request = UserJoinRequest.builder()
				.email("invalid-email-format")
				.password("password")
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when
			// then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUsersEntity(request),
				UserJoinValidateService.ERROR_MESSAGE_EMAIL_INVALID_FORMAT);

		}

		@Test
		@DisplayName("이메일이 중복되면 실패")
		void emailIsDuplicate() {
			// given
			String duplicateEmail = "test@test.com";
			UserJoinRequest request = UserJoinRequest.builder()
				.email(duplicateEmail)
				.password("password")
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when
			when(usersRepository.existsByEmail(duplicateEmail)).thenReturn(true);

			// then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUsersEntity(request),
				UserJoinValidateService.ERROR_MESSAGE_EMAIL_DUPLICATE);
		}

		@Test
		@DisplayName("비밀번호가 null 이면 실패")
		void passwordIsNull() {

			// given
			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@test.com")
				.password(null)
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when & then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUsersEntity(request),
				UserJoinValidateService.ERROR_MESSAGE_PASSWORD_INVALID_FORMAT);
		}

		// TODO 비밀번호 복호화 관련 테스트 케이스 추가 필요

		@ParameterizedTest
		@MethodSource("invalidPhoneNumberProvider")
		@DisplayName("전화번호 관련 실패 케이스 파라미터 테스트")
		void phoneNumberInvalidCases(String phoneNumber, String expectedErrorMessage) {
			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@test.com")
				.password("test")
				.nickname(null)
				.name("test")
				.phoneNumber(phoneNumber)
				.build();

			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUsersEntity(request),
				expectedErrorMessage);
		}

		static Stream<org.junit.jupiter.params.provider.Arguments> invalidPhoneNumberProvider() {
			return Stream.of(
				org.junit.jupiter.params.provider.Arguments.of(null,
					UserJoinValidateService.ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT),
				org.junit.jupiter.params.provider.Arguments.of("abc",
					UserJoinValidateService.ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT)
			);
		}

		// success test

	}

}
