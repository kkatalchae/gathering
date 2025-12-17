package com.gathering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * OAuth 클라이언트 설정 프로퍼티
 * application.yaml의 oauth.* 설정을 바인딩
 */
@Component
@ConfigurationProperties(prefix = "oauth")
@Getter
@Setter
public class OAuthClientProperties {

	private OAuthProviderProperties google;

	@Getter
	@Setter
	public static class OAuthProviderProperties {

		private String clientId;
		private String clientSecret;
		private String redirectUri;
	}
}