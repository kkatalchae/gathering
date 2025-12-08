package com.gathering.user;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.user.application.UserValidator;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;

/**
 * UserValidator 단위 테스트
 */
@SpringBootTest
@Transactional
class UserValidatorTest {

	@Autowired
	private UserValidator userValidator;

	@Autowired
	private UsersRepository usersRepository;

	@BeforeEach
	void setUp() {
		usersRepository.deleteAll();
	}

	@Nested
	@DisplayName("이메일 형식 검증")
	class EmailFormatValidation {

		@ParameterizedTest
		@ValueSource(strings = {"invalid-email", "@example.com", "test@", "test"})
		@DisplayName("잘못된 이메일 형식")
		void invalidEmailFormat(String invalidEmail) {
			// when & then
			assertThatThrownBy(() -> userValidator.validateEmailFormat(invalidEmail))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EMAIL_FORMAT);
		}

		@ParameterizedTest
		@ValueSource(strings = {"test@example.com", "user.name@example.co.kr", "test123@test.com"})
		@DisplayName("올바른 이메일 형식")
		void validEmailFormat(String validEmail) {
			// when & then
			assertThatCode(() -> userValidator.validateEmailFormat(validEmail))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("이메일 중복 검증")
	class EmailDuplicateValidation {

		@Test
		@DisplayName("이메일 중복")
		void duplicateEmail() {
			// given
			String email = "test@example.com";
			UsersEntity user = UsersEntity.builder()
				.email(email)
				.name("홍길동")
				.phoneNumber("01012345678")
				.build();
			usersRepository.save(user);

			// when & then
			assertThatThrownBy(() -> userValidator.validateEmailUnique(email))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATE);
		}

		@Test
		@DisplayName("이메일 사용 가능")
		void availableEmail() {
			// given
			String email = "available@example.com";

			// when & then
			assertThatCode(() -> userValidator.validateEmailUnique(email))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("전화번호 형식 검증")
	class PhoneNumberFormatValidation {

		@ParameterizedTest
		@ValueSource(strings = {"123", "010-1234-5678", "010123456789", "abcdefghij"})
		@DisplayName("잘못된 전화번호 형식")
		void invalidPhoneNumberFormat(String invalidPhone) {
			// when & then
			assertThatThrownBy(() -> userValidator.validatePhoneNumberFormat(invalidPhone))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PHONE_NUMBER_FORMAT);
		}

		@ParameterizedTest
		@ValueSource(strings = {"01012345678", "0212345678", "0101234567"})
		@DisplayName("올바른 전화번호 형식")
		void validPhoneNumberFormat(String validPhone) {
			// when & then
			assertThatCode(() -> userValidator.validatePhoneNumberFormat(validPhone))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("전화번호 중복 검증")
	class PhoneNumberDuplicateValidation {

		@Test
		@DisplayName("전화번호 중복")
		void duplicatePhoneNumber() {
			// given
			String phoneNumber = "01012345678";
			UsersEntity user = UsersEntity.builder()
				.email("test@example.com")
				.name("홍길동")
				.phoneNumber(phoneNumber)
				.build();
			usersRepository.save(user);

			// when & then
			assertThatThrownBy(() -> userValidator.validatePhoneNumberUnique(phoneNumber))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NUMBER_DUPLICATE);
		}

		@Test
		@DisplayName("전화번호 사용 가능")
		void availablePhoneNumber() {
			// given
			String phoneNumber = "01087654321";

			// when & then
			assertThatCode(() -> userValidator.validatePhoneNumberUnique(phoneNumber))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("비밀번호 형식 검증")
	class PasswordFormatValidation {

		@ParameterizedTest
		@ValueSource(strings = {"short", "nospecialchar1", "NoDigit!", "12345678"})
		@DisplayName("잘못된 비밀번호 형식")
		void invalidPasswordFormat(String invalidPassword) {
			// when & then
			assertThatThrownBy(() -> userValidator.validatePasswordFormat(invalidPassword))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_FORMAT);
		}

		@ParameterizedTest
		@ValueSource(strings = {"Password1!", "Valid123!", "Secure1@", "1234567!"})
		@DisplayName("올바른 비밀번호 형식")
		void validPasswordFormat(String validPassword) {
			// when & then
			assertThatCode(() -> userValidator.validatePasswordFormat(validPassword))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("이름 검증")
	class NameValidation {

		@Test
		@DisplayName("빈 이름")
		void blankName() {
			// when & then
			assertThatThrownBy(() -> userValidator.validateName("   "))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NAME_BLANK);
		}

		@Test
		@DisplayName("null 이름 (변경하지 않음)")
		void nullName() {
			// when & then
			assertThatCode(() -> userValidator.validateName(null))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("올바른 이름")
		void validName() {
			// when & then
			assertThatCode(() -> userValidator.validateName("홍길동"))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("회원가입 통합 검증")
	class JoinValidation {

		@Test
		@DisplayName("회원가입 성공")
		void validateForJoin_Success() {
			// given
			UserJoinRequest request = UserJoinRequest.builder()
				.email("test@example.com")
				.password("Password1!")
				.nickname("테스터")
				.name("홍길동")
				.phoneNumber("01012345678")
				.build();

			// when & then
			assertThatCode(() -> userValidator.validateForJoin(request))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("회원가입 실패 - 이메일 중복")
		void validateForJoin_DuplicateEmail() {
			// given
			String email = "test@example.com";
			UsersEntity user = UsersEntity.builder()
				.email(email)
				.name("기존유저")
				.phoneNumber("01011111111")
				.build();
			usersRepository.save(user);

			UserJoinRequest request = UserJoinRequest.builder()
				.email(email)
				.password("Password1!")
				.name("신규유저")
				.phoneNumber("01022222222")
				.build();

			// when & then
			assertThatThrownBy(() -> userValidator.validateForJoin(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATE);
		}

		@Test
		@DisplayName("회원가입 실패 - 전화번호 중복")
		void validateForJoin_DuplicatePhoneNumber() {
			// given
			String phoneNumber = "01012345678";
			UsersEntity user = UsersEntity.builder()
				.email("existing@example.com")
				.name("기존유저")
				.phoneNumber(phoneNumber)
				.build();
			usersRepository.save(user);

			UserJoinRequest request = UserJoinRequest.builder()
				.email("new@example.com")
				.password("Password1!")
				.name("신규유저")
				.phoneNumber(phoneNumber)
				.build();

			// when & then
			assertThatThrownBy(() -> userValidator.validateForJoin(request))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NUMBER_DUPLICATE);
		}
	}
}