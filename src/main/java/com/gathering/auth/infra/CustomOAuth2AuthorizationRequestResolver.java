package com.gathering.auth.infra;

import java.time.Duration;
import java.util.Optional;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.gathering.common.adapter.RedisAdapter;
import com.gathering.common.utility.CookieUtil;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 Authorization Request Resolver 커스터마이징
 * 소셜 연동 모드를 판별하기 위해 JWT 인증 여부를 확인하고 state를 Redis에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

	private static final String OAUTH_LINK_PREFIX = "oauth:link:";
	private static final Duration OAUTH_LINK_TTL = Duration.ofMinutes(5);
	private static final String OAUTH_MODE = "mode";

	private final ClientRegistrationRepository clientRegistrationRepository;
	private final RedisAdapter redisAdapter;
	private final JwtTokenProvider jwtTokenProvider;

	private OAuth2AuthorizationRequestResolver defaultResolver;

	@PostConstruct
	public void init() {
		this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
			clientRegistrationRepository,
			"/oauth2/authorization"
		);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
		return customizeAuthorizationRequest(request, authorizationRequest);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		OAuth2AuthorizationRequest authorizationRequest =
			defaultResolver.resolve(request, clientRegistrationId);
		return customizeAuthorizationRequest(request, authorizationRequest);
	}

	/**
	 * Authorization Request 커스터마이징
	 * JWT 인증된 사용자의 경우 연동 모드로 판단하여 state를 Redis에 저장
	 */
	private OAuth2AuthorizationRequest customizeAuthorizationRequest(
		HttpServletRequest request,
		OAuth2AuthorizationRequest authorizationRequest) {

		if (authorizationRequest == null) {
			return null;
		}

		Optional<String> refreshToken = CookieUtil.getCookie(request, AuthConstants.REFRESH_TOKEN_COOKIE);

		// 인증 헤더가 없음 -> 로그인, 회원가입 로직
		if (refreshToken.isPresent()
			&& Optional.ofNullable(request.getQueryString())
			.map(qs -> qs.contains(OAUTH_MODE))
			.orElse(false)
		) {
			setOAuthState(refreshToken.get(), authorizationRequest);
		}

		return authorizationRequest;
	}

	private void setOAuthState(String refreshToken, OAuth2AuthorizationRequest authorizationRequest) {

		// JWT에서 tsid 추출
		String tsid = jwtTokenProvider.getTsidFromToken(refreshToken);
		String state = authorizationRequest.getState();

		log.debug("OAuth link mode detected. Storing state={} for user={}", state, tsid);

		// Redis에 저장: oauth:link:{state} = {tsid}
		redisAdapter.set(OAUTH_LINK_PREFIX + state, tsid, OAUTH_LINK_TTL);

	}
}
