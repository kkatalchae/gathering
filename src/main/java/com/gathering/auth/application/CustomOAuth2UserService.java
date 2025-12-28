package com.gathering.auth.application;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.auth.domain.OAuthPrincipal;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.UserOAuthConnectionEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserService userService;
	private final UsersRepository usersRepository;
	private final UserOAuthConnectionRepository oauthConnectionRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		// 1. OAuth 제공자로부터 사용자 정보 추출
		Map<String, Object> attributes = callSuperLoadUser(userRequest).getAttributes();

		OAuthUserInfo oAuthUserInfo = OAuthUserInfo.of(
			userRequest.getClientRegistration().getRegistrationId(),
			attributes
		);

		// 2. 기존 연동 정보 확인 (provider + providerId)
		Optional<UserOAuthConnectionEntity> existingConnection =
			oauthConnectionRepository.findByProviderAndProviderId(
				oAuthUserInfo.getProvider(),
				oAuthUserInfo.getProviderId()
			);

		if (existingConnection.isPresent()) {
			// 2-1. 이미 연동된 계정 → 로그인 처리
			return handleExistingOAuthConnection(existingConnection.get(), attributes);
		}

		// 3. 이메일로 기존 사용자 확인
		Optional<UsersEntity> existingUser = usersRepository.findByEmail(oAuthUserInfo.getEmail());

		if (existingUser.isPresent()) {
			// 3-2. 이메일로 가입된 다른 계정 → 에러
			throw new BusinessException(ErrorCode.OAUTH_DIFFERENT_ACCOUNT);
		}

		// 3-1. 신규 회원가입 + 소셜 연동 정보 저장
		return handleNewOAuthUser(oAuthUserInfo, attributes);
	}

	/**
	 * 테스트를 위해 super.loadUser() 호출을 별도 메서드로 분리
	 */
	protected OAuth2User callSuperLoadUser(OAuth2UserRequest userRequest) {
		return super.loadUser(userRequest);
	}

	private OAuth2User handleExistingOAuthConnection(
		UserOAuthConnectionEntity connection,
		Map<String, Object> attributes
	) {
		UsersEntity user = usersRepository.findById(connection.getUserTsid())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return new OAuthPrincipal(user, attributes);
	}

	private OAuth2User handleNewOAuthUser(
		OAuthUserInfo oAuthUserInfo,
		Map<String, Object> attributes
	) {
		UsersEntity newUser = userService.socialJoin(oAuthUserInfo);
		UserOAuthConnectionEntity connection = UserOAuthConnectionEntity.from(
			newUser.getTsid(),
			oAuthUserInfo
		);
		oauthConnectionRepository.save(connection);
		return new OAuthPrincipal(newUser, attributes);
	}
}

