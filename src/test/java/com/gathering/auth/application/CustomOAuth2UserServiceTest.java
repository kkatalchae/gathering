package com.gathering.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.gathering.auth.domain.OAuthPrincipal;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.UserOAuthConnectionEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOAuth2UserService 테스트")
class CustomOAuth2UserServiceTest {

	@Spy
	@InjectMocks
	private CustomOAuth2UserService customOAuth2UserService;

	@Mock
	private UserService userService;

	@Mock
	private UsersRepository usersRepository;

	@Mock
	private UserOAuthConnectionRepository oauthConnectionRepository;

	private OAuth2UserRequest userRequest;
	private Map<String, Object> googleAttributes;

	@BeforeEach
	void setUp() {
		// Google attributes 준비 (Google과의 통신 성공 가정)
		googleAttributes = new HashMap<>();
		googleAttributes.put("sub", "google-user-123");
		googleAttributes.put("email", "test@gmail.com");
		googleAttributes.put("name", "홍길동");
		googleAttributes.put("picture", "https://example.com/profile.jpg");

		// OAuth2UserRequest 생성
		ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
			.clientId("test-client-id")
			.clientSecret("test-client-secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("profile", "email")
			.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
			.tokenUri("https://oauth2.googleapis.com/token")
			.userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
			.userNameAttributeName("email")
			.clientName("Google")
			.build();

		OAuth2AccessToken accessToken = new OAuth2AccessToken(
			OAuth2AccessToken.TokenType.BEARER,
			"mock-access-token",
			null,
			null
		);

		userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

		// super.loadUser() Mock - Google과의 통신이 성공했다고 가정
		OAuth2User mockOAuth2User = new DefaultOAuth2User(
			java.util.Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
			googleAttributes,
			"email"
		);
		doReturn(mockOAuth2User).when(customOAuth2UserService).callSuperLoadUser(any());
	}

	@Nested
	@DisplayName("소셜 로그인 처리")
	class SocialLoginHandling {

		@Test
		@DisplayName("기존_연동된_계정으로_로그인하면_해당_사용자의_OAuthPrincipal을_반환한다")
		void 기존_연동된_계정으로_로그인하면_해당_사용자의_OAuthPrincipal을_반환한다() {
			// given
			String userTsid = "01HQXYZ123456";

			UsersEntity existingUser = UsersEntity.builder()
				.tsid(userTsid)
				.email("test@gmail.com")
				.name("홍길동")
				.emailVerified(true)
				.build();

			UserOAuthConnectionEntity existingConnection = UserOAuthConnectionEntity.of(
				userTsid,
				OAuthProvider.GOOGLE,
				"google-user-123",
				"test@gmail.com"
			);

			when(oauthConnectionRepository.findByProviderAndProviderId(
				OAuthProvider.GOOGLE,
				"google-user-123"
			)).thenReturn(Optional.of(existingConnection));

			when(usersRepository.findById(userTsid)).thenReturn(Optional.of(existingUser));

			// when
			OAuth2User result = customOAuth2UserService.loadUser(userRequest);

			// then
			assertThat(result).isInstanceOf(OAuthPrincipal.class);
			OAuthPrincipal principal = (OAuthPrincipal)result;
			assertThat(principal.getName()).isEqualTo("test@gmail.com");
			assertThat(principal.getUsername()).isEqualTo(userTsid);

			verify(oauthConnectionRepository, times(1)).findByProviderAndProviderId(
				OAuthProvider.GOOGLE,
				"google-user-123"
			);
			verify(usersRepository, times(1)).findById(userTsid);
			verify(usersRepository, never()).findByEmail(anyString());
			verify(userService, never()).socialJoin(any());
		}

		@Test
		@DisplayName("신규_사용자_회원가입_후_연동정보를_저장하고_OAuthPrincipal을_반환한다")
		void 신규_사용자_회원가입_후_연동정보를_저장하고_OAuthPrincipal을_반환한다() {
			// given
			UsersEntity newUser = UsersEntity.builder()
				.tsid("01HQXYZ999999")
				.email("test@gmail.com")
				.name("홍길동")
				.emailVerified(true)
				.build();

			when(oauthConnectionRepository.findByProviderAndProviderId(
				OAuthProvider.GOOGLE,
				"google-user-123"
			)).thenReturn(Optional.empty());

			when(usersRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
			when(userService.socialJoin(any())).thenReturn(newUser);

			// when
			OAuth2User result = customOAuth2UserService.loadUser(userRequest);

			// then
			assertThat(result).isInstanceOf(OAuthPrincipal.class);
			OAuthPrincipal principal = (OAuthPrincipal)result;
			assertThat(principal.getName()).isEqualTo("test@gmail.com");
			assertThat(principal.getUsername()).isEqualTo("01HQXYZ999999");

			verify(oauthConnectionRepository, times(1)).findByProviderAndProviderId(
				OAuthProvider.GOOGLE,
				"google-user-123"
			);
			verify(usersRepository, times(1)).findByEmail("test@gmail.com");
			verify(userService, times(1)).socialJoin(any());
			verify(oauthConnectionRepository, times(1)).save(any(UserOAuthConnectionEntity.class));
		}

		@Test
		@DisplayName("이메일_중복_다른_계정이_있으면_OAUTH_DIFFERENT_ACCOUNT_예외가_발생한다")
		void 이메일_중복_다른_계정이_있으면_OAUTH_DIFFERENT_ACCOUNT_예외가_발생한다() {
			// given
			UsersEntity existingUser = UsersEntity.builder()
				.tsid("01HQXYZ123456")
				.email("test@gmail.com")
				.name("기존사용자")
				.emailVerified(false) // 일반 회원가입 사용자
				.build();

			when(oauthConnectionRepository.findByProviderAndProviderId(
				OAuthProvider.GOOGLE,
				"google-user-123"
			)).thenReturn(Optional.empty());

			when(usersRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));

			// when & then
			assertThatThrownBy(() -> customOAuth2UserService.loadUser(userRequest))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_DIFFERENT_ACCOUNT);

			verify(oauthConnectionRepository, times(1)).findByProviderAndProviderId(
				OAuthProvider.GOOGLE,
				"google-user-123"
			);
			verify(usersRepository, times(1)).findByEmail("test@gmail.com");
			verify(userService, never()).socialJoin(any());
			verify(oauthConnectionRepository, never()).save(any());
		}
	}
}