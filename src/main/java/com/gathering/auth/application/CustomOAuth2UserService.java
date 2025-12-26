package com.gathering.auth.application;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.gathering.auth.domain.OAuthPrincipal;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserService userService;
	private final UsersRepository usersRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		Map<String, Object> attributes = super.loadUser(userRequest).getAttributes();

		OAuthUserInfo oAuthUserInfo = OAuthUserInfo.of(
			userRequest.getClientRegistration().getRegistrationId(),
			attributes
		);

		if (usersRepository.findByEmail(oAuthUserInfo.getEmail()).isPresent()) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}

		return new OAuthPrincipal(userService.socialJoin(oAuthUserInfo), attributes);
	}
}
