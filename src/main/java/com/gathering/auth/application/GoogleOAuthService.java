package com.gathering.auth.application;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.user.domain.model.OAuthProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService implements OAuthService {

	private final ClientRegistrationRepository clientRegistrationRepository;
	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public String generateAuthorizationUrl(String state) {
		ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");

		return UriComponentsBuilder
			.fromUriString(google.getProviderDetails().getAuthorizationUri())
			.queryParam("client_id", google.getClientId())
			.queryParam("redirect_uri", google.getRedirectUri())
			.queryParam("response_type", "code")
			.queryParam("scope", String.join(" ", google.getScopes()))
			.queryParam("state", state)
			.build()
			.toUriString();
	}

	@Override
	public OAuthUserInfo getUserInfo(String code, String state) {
		ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");

		String accessToken = exchangeCodeForToken(google, code);
		return fetchUserInfo(google, accessToken);
	}

	@Override
	public OAuthProvider getProvider() {
		return OAuthProvider.GOOGLE;
	}

	private String exchangeCodeForToken(ClientRegistration google, String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", google.getClientId());
		params.add("client_secret", google.getClientSecret());
		params.add("redirect_uri", google.getRedirectUri());
		params.add("code", code);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(
				google.getProviderDetails().getTokenUri(),
				request,
				Map.class
			);

			Map<String, Object> body = response.getBody();
			if (body == null || !body.containsKey("access_token")) {
				throw new BusinessException(ErrorCode.OAUTH_CODE_EXCHANGE_FAILED);
			}

			return (String)body.get("access_token");
		} catch (Exception e) {
			log.error("Google 토큰 교환 실패: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.OAUTH_CODE_EXCHANGE_FAILED);
		}
	}

	private OAuthUserInfo fetchUserInfo(ClientRegistration google, String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<Map> response = restTemplate.exchange(
				google.getProviderDetails().getUserInfoEndpoint().getUri(),
				HttpMethod.GET,
				request,
				Map.class
			);

			Map<String, Object> userAttributes = response.getBody();
			if (userAttributes == null) {
				throw new BusinessException(ErrorCode.OAUTH_USER_INFO_FETCH_FAILED);
			}

			return OAuthUserInfo.builder()
				.provider(OAuthProvider.GOOGLE)
				.providerId((String)userAttributes.get("sub"))
				.email((String)userAttributes.get("email"))
				.emailVerified((Boolean)userAttributes.getOrDefault("email_verified", false))
				.name((String)userAttributes.get("name"))
				.profileImageUrl((String)userAttributes.get("picture"))
				.build();
		} catch (Exception e) {
			log.error("Google 사용자 정보 조회 실패: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.OAUTH_USER_INFO_FETCH_FAILED);
		}
	}
}