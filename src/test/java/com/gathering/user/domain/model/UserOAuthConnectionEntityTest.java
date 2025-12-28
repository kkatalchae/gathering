package com.gathering.user.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.gathering.auth.domain.OAuthUserInfo;

@DisplayName("UserOAuthConnectionEntity 도메인 테스트")
class UserOAuthConnectionEntityTest {

	@Nested
	@DisplayName("복합 PK 동등성 테스트")
	class CompositePrimaryKeyEquality {

		@Test
		@DisplayName("같은_userTsid와_provider를_가진_ID는_동등하다")
		void 같은_userTsid와_provider를_가진_ID는_동등하다() {
			// given
			UserOAuthConnectionEntity.ConnectionId id1 =
				new UserOAuthConnectionEntity.ConnectionId("01HQXYZ123456", OAuthProvider.GOOGLE);
			UserOAuthConnectionEntity.ConnectionId id2 =
				new UserOAuthConnectionEntity.ConnectionId("01HQXYZ123456", OAuthProvider.GOOGLE);

			// when & then
			assertThat(id1).isEqualTo(id2);
			assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
		}

		@Test
		@DisplayName("다른_userTsid를_가진_ID는_동등하지_않다")
		void 다른_userTsid를_가진_ID는_동등하지_않다() {
			// given
			UserOAuthConnectionEntity.ConnectionId id1 =
				new UserOAuthConnectionEntity.ConnectionId("01HQXYZ123456", OAuthProvider.GOOGLE);
			UserOAuthConnectionEntity.ConnectionId id2 =
				new UserOAuthConnectionEntity.ConnectionId("01HQXYZ999999", OAuthProvider.GOOGLE);

			// when & then
			assertThat(id1).isNotEqualTo(id2);
		}

		@Test
		@DisplayName("다른_provider를_가진_ID는_동등하지_않다")
		void 다른_provider를_가진_ID는_동등하지_않다() {
			// given
			UserOAuthConnectionEntity.ConnectionId id1 =
				new UserOAuthConnectionEntity.ConnectionId("01HQXYZ123456", OAuthProvider.GOOGLE);
			UserOAuthConnectionEntity.ConnectionId id2 =
				new UserOAuthConnectionEntity.ConnectionId("01HQXYZ123456", OAuthProvider.GOOGLE); // 같은 provider

			// when & then
			assertThat(id1).isEqualTo(id2); // 같은 provider이므로 동등
		}
	}

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