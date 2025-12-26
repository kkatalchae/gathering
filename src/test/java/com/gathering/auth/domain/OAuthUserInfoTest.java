package com.gathering.auth.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.UsersEntity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("OAuthUserInfo 도메인 테스트")
class OAuthUserInfoTest {

	@Nested
	@DisplayName("Google OAuth 사용자 정보 변환")
	class GoogleOAuthUserInfoConversion {

		@Test
		@DisplayName("유효한_Google_attributes를_OAuthUserInfo로_변환한다")
		void 유효한_Google_attributes를_OAuthUserInfo로_변환한다() {
			// given
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("sub", "google-user-123");
			attributes.put("email", "test@gmail.com");
			attributes.put("name", "홍길동");
			attributes.put("picture", "https://example.com/profile.jpg");

			// when
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.of("google", attributes);

			// then
			assertThat(oAuthUserInfo).isNotNull();
			assertThat(oAuthUserInfo.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
			assertThat(oAuthUserInfo.getProviderId()).isEqualTo("google-user-123");
			assertThat(oAuthUserInfo.getEmail()).isEqualTo("test@gmail.com");
			assertThat(oAuthUserInfo.getName()).isEqualTo("홍길동");
			assertThat(oAuthUserInfo.getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
		}

		@Test
		@DisplayName("OAuthUserInfo를_UsersEntity로_변환하면_emailVerified가_true이다")
		void OAuthUserInfo를_UsersEntity로_변환하면_emailVerified가_true이다() {
			// given
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.email("test@gmail.com")
				.name("홍길동")
				.profileImageUrl("https://example.com/profile.jpg")
				.build();

			// when
			UsersEntity usersEntity = OAuthUserInfo.toUsersEntity(oAuthUserInfo);

			// then
			assertThat(usersEntity).isNotNull();
			assertThat(usersEntity.getEmail()).isEqualTo("test@gmail.com");
			assertThat(usersEntity.getName()).isEqualTo("홍길동");
			assertThat(usersEntity.getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
		}
	}

	@Nested
	@DisplayName("지원하지 않는 OAuth 제공자")
	class UnsupportedOAuthProvider {

		@Test
		@DisplayName("지원하지_않는_OAuth_제공자로_변환_시_OAUTH_PROVIDER_NOT_SUPPORTED_예외가_발생한다")
		void 지원하지_않는_OAuth_제공자로_변환_시_OAUTH_PROVIDER_NOT_SUPPORTED_예외가_발생한다() {
			// given
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("id", "kakao-user-123");
			attributes.put("email", "test@kakao.com");

			// when & then
			assertThatThrownBy(() -> OAuthUserInfo.of("kakao", attributes))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
		}
	}

	@Nested
	@DisplayName("필수 attributes 검증")
	class AttributesValidation {

		private Validator validator;

		@BeforeEach
		void setUp() {
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
			validator = factory.getValidator();
		}

		@Test
		@DisplayName("email이_null인_경우_validation_제약_위반이_발생한다")
		void email이_null인_경우_validation_제약_위반이_발생한다() {
			// given
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.name("홍길동")
				// email 누락
				.build();

			// when
			Set<ConstraintViolation<OAuthUserInfo>> violations = validator.validate(oAuthUserInfo);

			// then
			assertThat(violations).isNotEmpty();
			assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
		}

		@Test
		@DisplayName("name이_null인_경우_validation_제약_위반이_발생한다")
		void name이_null인_경우_validation_제약_위반이_발생한다() {
			// given
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.email("test@gmail.com")
				// name 누락
				.build();

			// when
			Set<ConstraintViolation<OAuthUserInfo>> violations = validator.validate(oAuthUserInfo);

			// then
			assertThat(violations).isNotEmpty();
			assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
		}

		@Test
		@DisplayName("email이_빈_문자열인_경우_validation_제약_위반이_발생한다")
		void email이_빈_문자열인_경우_validation_제약_위반이_발생한다() {
			// given
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.email("")
				.name("홍길동")
				.build();

			// when
			Set<ConstraintViolation<OAuthUserInfo>> violations = validator.validate(oAuthUserInfo);

			// then
			assertThat(violations).isNotEmpty();
			assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
		}

		@Test
		@DisplayName("name이_빈_문자열인_경우_validation_제약_위반이_발생한다")
		void name이_빈_문자열인_경우_validation_제약_위반이_발생한다() {
			// given
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.email("test@gmail.com")
				.name("")
				.build();

			// when
			Set<ConstraintViolation<OAuthUserInfo>> violations = validator.validate(oAuthUserInfo);

			// then
			assertThat(violations).isNotEmpty();
			assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
		}

		@Test
		@DisplayName("profileImageUrl이_null이어도_validation_제약_위반이_발생하지_않는다")
		void profileImageUrl이_null이어도_validation_제약_위반이_발생하지_않는다() {
			// given
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.email("test@gmail.com")
				.name("홍길동")
				// profileImageUrl 누락
				.build();

			// when
			Set<ConstraintViolation<OAuthUserInfo>> violations = validator.validate(oAuthUserInfo);

			// then
			assertThat(violations).isEmpty();
		}
	}
}
