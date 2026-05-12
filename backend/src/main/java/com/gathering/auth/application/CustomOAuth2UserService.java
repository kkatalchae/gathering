package com.gathering.auth.application;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gathering.auth.domain.OAuthPrincipal;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.auth.infra.AuthConstants;
import com.gathering.common.adapter.RedisAdapter;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.UserOAuthConnectionEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserService userService;
	private final UsersRepository usersRepository;
	private final UserOAuthConnectionRepository oauthConnectionRepository;
	private final RedisAdapter redisAdapter;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		// OAuth 제공자로부터 사용자 정보 추출
		Map<String, Object> attributes = callSuperLoadUser(userRequest).getAttributes();

		OAuthUserInfo oAuthUserInfo = OAuthUserInfo.of(userRequest.getClientRegistration().getRegistrationId(),
			attributes);

		// 연동 모드 판별 (state 파라미터 확인)
		String linkingUserTsid = getLinkingUserTsid();

		if (linkingUserTsid != null) {
			// 연동 모드
			log.debug("OAuth link mode detected for user={}", linkingUserTsid);
			return handleOAuthLink(linkingUserTsid, oAuthUserInfo, attributes);
		}

		return handleOAuthLogin(oAuthUserInfo, attributes);
	}

	/**
	 * 테스트를 위해 super.loadUser() 호출을 별도 메서드로 분리
	 */
	protected OAuth2User callSuperLoadUser(OAuth2UserRequest userRequest) {
		return super.loadUser(userRequest);
	}

	private OAuth2User handleExistingOAuthConnection(UserOAuthConnectionEntity connection,
		Map<String, Object> attributes) {
		UsersEntity user = usersRepository.findById(connection.getUserTsid())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		return new OAuthPrincipal(user, attributes);
	}

	private OAuth2User handleNewOAuthUser(OAuthUserInfo oAuthUserInfo, Map<String, Object> attributes) {
		UsersEntity newUser = userService.socialJoin(oAuthUserInfo);
		UserOAuthConnectionEntity connection = UserOAuthConnectionEntity.from(newUser.getTsid(), oAuthUserInfo);
		oauthConnectionRepository.save(connection);
		return new OAuthPrincipal(newUser, attributes);
	}

	/**
	 * 연동 모드 판별: state 파라미터로부터 연동할 사용자 TSID 조회
	 *
	 * @return 연동 모드면 사용자 TSID, 아니면 null
	 */
	private String getLinkingUserTsid() {
		ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();

		if (attrs == null) {
			return null;
		}

		String state = attrs.getRequest().getParameter("state");
		if (state == null) {
			return null;
		}

		Optional<String> tsid = redisAdapter.get(AuthConstants.OAUTH_LINK_PREFIX + state);
		if (tsid.isPresent()) {
			// 일회성 사용 후 삭제
			redisAdapter.delete(AuthConstants.OAUTH_LINK_PREFIX + state);
			return tsid.get();
		}
		return null;
	}

	/**
	 * OAuth 연동 처리 (이미 로그인된 사용자가 소셜 계정 연동)
	 *
	 * @param userTsid 연동할 사용자 TSID
	 * @param oAuthUserInfo OAuth 사용자 정보
	 * @param attributes OAuth attributes
	 * @return OAuthPrincipal (linkMode = true)
	 */
	private OAuth2User handleOAuthLink(String userTsid, OAuthUserInfo oAuthUserInfo, Map<String, Object> attributes) {

		// 1. 사용자 존재 확인
		UsersEntity user = usersRepository.findById(userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 2. 이미 연동되어 있는지 확인
		Optional<UserOAuthConnectionEntity> existingConnection = oauthConnectionRepository.findByProviderAndProviderId(
			oAuthUserInfo.getProvider(), oAuthUserInfo.getProviderId());

		if (existingConnection.isPresent()) {
			UserOAuthConnectionEntity connection = existingConnection.get();

			// 동일한 사용자가 재연동 시도 → 이미 연동되어 있으므로 성공 처리
			if (connection.getUserTsid().equals(userTsid)) {
				log.debug("OAuth account already linked. user={}, provider={}", userTsid, oAuthUserInfo.getProvider());
				return new OAuthPrincipal(user, attributes, true);
			}

			// 다른 사용자가 이미 사용 중인 경우 → 에러
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ALREADY_USED);
		}

		// 3. 연동 정보 저장
		UserOAuthConnectionEntity connection = UserOAuthConnectionEntity.from(userTsid, oAuthUserInfo);
		oauthConnectionRepository.save(connection);

		log.info("OAuth account linked successfully. user={}, provider={}", userTsid, oAuthUserInfo.getProvider());

		// 4. OAuthPrincipal 반환 (linkMode = true)
		return new OAuthPrincipal(user, attributes, true);
	}

	private OAuth2User handleOAuthLogin(OAuthUserInfo oAuthUserInfo, Map<String, Object> attributes) {
		// 기존 연동 정보 확인 (provider + providerId)
		Optional<UserOAuthConnectionEntity> existingConnection = oauthConnectionRepository.findByProviderAndProviderId(
			oAuthUserInfo.getProvider(), oAuthUserInfo.getProviderId());

		// 이메일로 기존 사용자 확인

		if (existingConnection.isPresent()) {
			// 이미 연동된 계정 → 로그인 처리
			return handleExistingOAuthConnection(existingConnection.get(), attributes);
		}

		Optional<UsersEntity> existingUser = usersRepository.findByEmail(oAuthUserInfo.getEmail());

		if (existingUser.isPresent()) {
			// 이메일로 가입된 다른 계정 → 에러
			throw new BusinessException(ErrorCode.OAUTH_DIFFERENT_ACCOUNT);
		}

		// 신규 회원가입 + 소셜 연동 정보 저장
		return handleNewOAuthUser(oAuthUserInfo, attributes);
	}
}

