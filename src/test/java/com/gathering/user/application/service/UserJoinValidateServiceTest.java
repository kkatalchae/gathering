package com.gathering.user.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;
import com.gathering.util.CryptoUtil;

@ExtendWith(MockitoExtension.class)
class UserJoinValidateServiceTest {

	private final String dummyAesKey = "1234567890123456"; // 16자 키

	@InjectMocks
	private UserJoinValidateService userJoinValidateService;

	@Mock
	private UsersRepository usersRepository;

	@BeforeEach
	void setUp() throws Exception {
		Field aesKeyField = UserJoinValidateService.class.getDeclaredField("aesKey");
		aesKeyField.setAccessible(true);
		aesKeyField.set(userJoinValidateService, dummyAesKey);
	}

	@Nested
	@DisplayName("회원가입 유효성 검증")
	class JoinValidate {

		// fail test
		@ParameterizedTest
		@MethodSource("invalidEmailProvider")
		@DisplayName("실패 - 올바르지 않은 이메일 형식 체크")
		void isEmailInvalidFormat(String email, String expectedErrorMessage) {
			// given
			UserJoinRequest request = UserJoinRequest.builder()
				.email(email)
				.password("password")
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when & then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUser(request),
				expectedErrorMessage);
		}

		static Stream<Arguments> invalidEmailProvider() {
			return Stream.of(
				// null
				Arguments.of(null,
					UserJoinValidateService.ERROR_MESSAGE_EMAIL_INVALID_FORMAT),
				// @ 미포함
				Arguments.of("test",
					UserJoinValidateService.ERROR_MESSAGE_EMAIL_INVALID_FORMAT),
				// . 미포함
				Arguments.of("test@",
					UserJoinValidateService.ERROR_MESSAGE_EMAIL_INVALID_FORMAT),
				// . 뒤에 없음
				Arguments.of("test@test.",
					UserJoinValidateService.ERROR_MESSAGE_EMAIL_INVALID_FORMAT)
			);
		}

		@Test
		@DisplayName("실패 - 이메일 중복")
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
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUser(request),
				UserJoinValidateService.ERROR_MESSAGE_EMAIL_DUPLICATE);
		}

		@ParameterizedTest
		@MethodSource("invalidPhoneNumberProvider")
		@DisplayName("실패 - 올바르지 않은 전화번호 형식")
		void isPhoneNumberInvalidFormat(String phoneNumber, String expectedErrorMessage) {
			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@test.com")
				.password("test")
				.nickname(null)
				.name("test")
				.phoneNumber(phoneNumber)
				.build();

			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUser(request),
				expectedErrorMessage);
		}

		static Stream<Arguments> invalidPhoneNumberProvider() {
			return Stream.of(
				// null
				Arguments.of(null,
					UserJoinValidateService.ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT),
				// 숫자가 아닌 문자
				Arguments.of("abc",
					UserJoinValidateService.ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT),
				// 10-11 자리가 아님
				Arguments.of("010",
					UserJoinValidateService.ERROR_MESSAGE_PHONE_NUMBER_INVALID_FORMAT)
			);
		}

		@Test
		@DisplayName("실패 - 전화번호 중복")
		void isPhoneNumberDuplicate() {
			// given
			String duplicatePhoneNumber = "01012341234";
			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@test.com")
				.password("password")
				.nickname(null)
				.name("test")
				.phoneNumber(duplicatePhoneNumber)
				.build();

			// when
			when(usersRepository.existsByPhoneNumber(duplicatePhoneNumber)).thenReturn(true);

			// then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUser(request),
				UserJoinValidateService.ERROR_MESSAGE_PHONE_NUMBER_DUPLICATE);
		}

		@ParameterizedTest
		@MethodSource("invalidPasswordProvider")
		@DisplayName("실패 - 올바르지 않은 비밀번호 형식")
		void isPasswordInvalidFormat(String password, String expectedErrorMessage) {

			// given
			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@test.com")
				.password(password)
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when & then
			assertThrows(IllegalArgumentException.class, () -> userJoinValidateService.validateUser(request),
				expectedErrorMessage);
		}

		static Stream<Arguments> invalidPasswordProvider() {
			return Stream.of(
				// null
				Arguments.of(null,
					UserJoinValidateService.ERROR_MESSAGE_PASSWORD_INVALID_FORMAT),
				// 8자 미만
				Arguments.of("Pass1!",
					UserJoinValidateService.ERROR_MESSAGE_PASSWORD_INVALID_FORMAT),
				// 특수문자 없음
				Arguments.of("Password1",
					UserJoinValidateService.ERROR_MESSAGE_PASSWORD_INVALID_FORMAT),
				// 숫자 없음
				Arguments.of("Password!",
					UserJoinValidateService.ERROR_MESSAGE_PASSWORD_INVALID_FORMAT)
			);
		}

		// success test
		@Test
		@DisplayName("성공 - 올바른 형식의 회원가입 요청")
		void success() {
			// given
			String password = "Password1*";
			String encryptedPassword = null;
			try {
				encryptedPassword = CryptoUtil.encryptAES(password, dummyAesKey);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@test.com")
				.password(encryptedPassword)
				.nickname(null)
				.name("test")
				.phoneNumber("01012341234")
				.build();

			// when & then
			assertDoesNotThrow(() -> userJoinValidateService.validateUser(request));

		}
	}
}
