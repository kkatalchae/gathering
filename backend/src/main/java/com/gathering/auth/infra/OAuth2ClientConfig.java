package com.gathering.auth.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import lombok.RequiredArgsConstructor;

/**
 * OAuth 2.0 Client 설정
 * Java 코드 기반으로 ClientRegistration을 생성하여 타입 안정성 확보
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2ClientConfig {

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String googleClientId;
	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String googleClientSecret;

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(this.googleClientRegistration());
	}

	/**
	 * Google OAuth 2.0 ClientRegistration 생성
	 * 참조: https://developers.google.com/identity/openid-connect/openid-connect?hl=ko#setredirecturi
	 */
	private ClientRegistration googleClientRegistration() {
		return ClientRegistration.withRegistrationId("google")
			.clientId(googleClientId)
			.clientSecret(googleClientSecret)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("profile", "email")
			.userNameAttributeName("email")
			.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
			.tokenUri("https://oauth2.googleapis.com/token")
			.userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
			.clientName("Google")
			.build();
	}
}
