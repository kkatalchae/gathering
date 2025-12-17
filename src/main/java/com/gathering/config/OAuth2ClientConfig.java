package com.gathering.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import lombok.RequiredArgsConstructor;

/**
 * OAuth 2.0 Client 설정
 * Java 코드 기반으로 ClientRegistration을 생성하여 타입 안정성 확보
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2ClientConfig {

	private final OAuthClientProperties properties;

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		List<ClientRegistration> registrations = new ArrayList<>();

		if (properties.getGoogle() != null) {
			registrations.add(googleClientRegistration(properties.getGoogle()));
		}

		return new InMemoryClientRegistrationRepository(registrations);
	}

	private ClientRegistration googleClientRegistration(OAuthClientProperties.OAuthProviderProperties props) {
		return ClientRegistration.withRegistrationId("google")
			.clientId(props.getClientId())
			.clientSecret(props.getClientSecret())
			.redirectUri(props.getRedirectUri())
			.scope("openid", "email", "profile")
			.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
			.tokenUri("https://oauth2.googleapis.com/token")
			.userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
			.userNameAttributeName("sub")
			.clientName("Google")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.build();
	}
}