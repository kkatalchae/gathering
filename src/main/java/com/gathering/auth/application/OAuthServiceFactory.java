package com.gathering.auth.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.user.domain.model.OAuthProvider;

import lombok.RequiredArgsConstructor;

/**
 * OAuth 서비스 팩토리
 * OAuth 제공자별로 적절한 서비스 구현체를 반환
 */
@Service
@RequiredArgsConstructor
public class OAuthServiceFactory {

	private final List<OAuthService> oauthServices;
	private final Map<OAuthProvider, OAuthService> serviceMap;

	public OAuthServiceFactory(List<OAuthService> oauthServices) {
		this.oauthServices = oauthServices;
		this.serviceMap = oauthServices.stream()
			.collect(Collectors.toMap(OAuthService::getProvider, Function.identity()));
	}

	public OAuthService getService(OAuthProvider provider) {
		OAuthService service = serviceMap.get(provider);
		if (service == null) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
		}
		return service;
	}

	/**
	 * String provider를 OAuthProvider enum으로 변환
	 * Controller에서 PathVariable을 받을 때 사용
	 *
	 * @param provider OAuth 제공자 문자열 (예: "google", "kakao")
	 * @return OAuthProvider enum
	 * @throws BusinessException 지원하지 않는 제공자인 경우
	 */
	public OAuthProvider parseProvider(String provider) {
		try {
			return OAuthProvider.valueOf(provider.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
		}
	}
}