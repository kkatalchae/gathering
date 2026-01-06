package com.gathering.user.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.gathering.auth.domain.OAuthUserInfo;

@DisplayName("UserOAuthConnectionEntity 도메인 테스트")
class UserOAuthConnectionEntityTest {

	@Nested
	@DisplayName("팩토리 메서드 테스트")
	class FactoryMethodTest {

		@Test
		@DisplayName("of_메서드로_UserOAuthConnectionEntity를_생성한다")
		void of_메서드로_UserOAuthConnectionEntity를_생성한다() {
			// given
			String userTsid = "01HQXYZ123456";
			OAuthProvider provider = OAuthProvider.GOOGLE;
			String providerId = "google-user-123";
			String email = "test@gmail.com";

			// when
			UserOAuthConnectionEntity connection = UserOAuthConnectionEntity.of(
				userTsid,
				provider,
				providerId,
				email
			);

			// then
			assertThat(connection).isNotNull();
			assertThat(connection.getUserTsid()).isEqualTo(userTsid);
			assertThat(connection.getProvider()).isEqualTo(provider);
			assertThat(connection.getProviderId()).isEqualTo(providerId);
			assertThat(connection.getEmail()).isEqualTo(email);
		}

		@Test
		@DisplayName("from_메서드로_OAuthUserInfo로부터_UserOAuthConnectionEntity를_생성한다")
		void from_메서드로_OAuthUserInfo로부터_UserOAuthConnectionEntity를_생성한다() {
			// given
			String userTsid = "01HQXYZ123456";
			OAuthUserInfo oAuthUserInfo = OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId("google-user-123")
				.email("test@gmail.com")
				.name("홍길동")
				.profileImageUrl("https://example.com/profile.jpg")
				.build();

			// when
			UserOAuthConnectionEntity connection = UserOAuthConnectionEntity.from(
				userTsid,
				oAuthUserInfo
			);

			// then
			assertThat(connection).isNotNull();
			assertThat(connection.getUserTsid()).isEqualTo(userTsid);
			assertThat(connection.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
			assertThat(connection.getProviderId()).isEqualTo("google-user-123");
			assertThat(connection.getEmail()).isEqualTo("test@gmail.com");
		}
	}
}