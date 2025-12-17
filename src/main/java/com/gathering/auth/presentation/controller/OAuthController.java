package com.gathering.auth.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.application.OAuthService;
import com.gathering.auth.application.OAuthServiceFactory;
import com.gathering.auth.application.OAuthStateService;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.auth.presentation.dto.AuthorizationUrlResponse;
import com.gathering.auth.presentation.dto.LoginResponse;
import com.gathering.auth.presentation.dto.OAuthCallbackRequest;
import com.gathering.auth.presentation.dto.OAuthLinkRequest;
import com.gathering.user.application.OAuthUserService;
import com.gathering.user.domain.model.OAuthProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth 인증 컨트롤러
 * Google OAuth 2.0 로그인 및 계정 연동 처리
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

	private final OAuthServiceFactory oauthServiceFactory;
	private final OAuthStateService oauthStateService;
	private final AuthService authService;
	private final OAuthUserService oauthUserService;

	/**
	 * OAuth 인증 URL 생성
	 * 사용자를 OAuth 제공자 로그인 페이지로 리다이렉트하기 위한 URL 반환
	 *
	 * @param provider OAuth 제공자 (예: google)
	 * @param request HttpServletRequest
	 * @return OAuth 인증 URL
	 */
	@GetMapping("/{provider}/authorize")
	public ResponseEntity<AuthorizationUrlResponse> authorize(
		@PathVariable String provider,
		HttpServletRequest request
	) {
		// 1. Provider 파싱
		OAuthProvider oauthProvider = oauthServiceFactory.parseProvider(provider);
		OAuthService oauthService = oauthServiceFactory.getService(oauthProvider);

		// 2. 현재 사용자 정보 추출 (비로그인 시 null)
		String currentUserTsid = authService.getCurrentUserTsidOrNull(request);
		String currentRefreshTokenJti = authService.getRefreshTokenJtiOrNull(request);

		// 3. State 생성 및 Redis 저장
		String state = oauthStateService.generateState(currentUserTsid, currentRefreshTokenJti);

		// 4. OAuth 인증 URL 생성
		String authorizationUrl = oauthService.generateAuthorizationUrl(state);

		return ResponseEntity.ok(AuthorizationUrlResponse.builder()
			.authorizationUrl(authorizationUrl)
			.build());
	}

	/**
	 * OAuth 콜백 처리 (회원가입/로그인)
	 * OAuth 제공자로부터 리다이렉트된 후 인증 코드를 받아 로그인 처리
	 *
	 * @param provider OAuth 제공자 (예: google)
	 * @param callbackRequest 콜백 요청 (code, state)
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return 로그인 응답 (AccessToken 포함)
	 */
	@PostMapping("/{provider}/callback")
	public ResponseEntity<LoginResponse> callback(
		@PathVariable String provider,
		@Valid @RequestBody OAuthCallbackRequest callbackRequest,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		// 1. Provider 파싱
		OAuthProvider oauthProvider = oauthServiceFactory.parseProvider(provider);
		OAuthService oauthService = oauthServiceFactory.getService(oauthProvider);

		// 2. State 및 Session 검증
		String currentUserTsid = authService.getCurrentUserTsidOrNull(request);
		String currentRefreshTokenJti = authService.getRefreshTokenJtiOrNull(request);
		oauthStateService.validateStateAndSessionOrThrow(
			callbackRequest.getState(),
			currentUserTsid,
			currentRefreshTokenJti
		);

		// 3. OAuth 사용자 정보 조회
		OAuthUserInfo oauthUserInfo = oauthService.getUserInfo(
			callbackRequest.getCode(),
			callbackRequest.getState()
		);

		// 4. OAuth 로그인 처리 (회원가입 또는 로그인)
		LoginResponse loginResponse = authService.loginWithOAuth(oauthUserInfo, response);

		return ResponseEntity.ok(loginResponse);
	}

	/**
	 * OAuth 계정 연동
	 * 로그인한 사용자가 OAuth 계정을 연동
	 *
	 * @param provider OAuth 제공자 (예: google)
	 * @param linkRequest 연동 요청 (code, state)
	 * @param request HttpServletRequest
	 * @return 200 OK
	 */
	@PostMapping("/{provider}/link")
	public ResponseEntity<Void> link(
		@PathVariable String provider,
		@Valid @RequestBody OAuthLinkRequest linkRequest,
		HttpServletRequest request
	) {
		// 1. Provider 파싱
		OAuthProvider oauthProvider = oauthServiceFactory.parseProvider(provider);
		OAuthService oauthService = oauthServiceFactory.getService(oauthProvider);

		// 2. 현재 로그인한 사용자 정보 추출 (로그인 필수)
		String currentUserTsid = authService.getCurrentUserTsid(request);
		String currentRefreshTokenJti = authService.getRefreshTokenJtiOrNull(request);

		// 3. State 및 Session 검증
		oauthStateService.validateStateAndSessionOrThrow(
			linkRequest.getState(),
			currentUserTsid,
			currentRefreshTokenJti
		);

		// 4. OAuth 사용자 정보 조회
		OAuthUserInfo oauthUserInfo = oauthService.getUserInfo(
			linkRequest.getCode(),
			linkRequest.getState()
		);

		// 5. 현재 사용자에게 OAuth 계정 연동
		oauthUserService.linkOAuthAccount(currentUserTsid, oauthUserInfo);

		return ResponseEntity.ok().build();
	}

	/**
	 * OAuth 계정 연동 해제
	 * 로그인한 사용자의 OAuth 계정 연동을 해제
	 * 마지막 인증 수단은 해제 불가
	 *
	 * @param provider OAuth 제공자 (예: google)
	 * @param request HttpServletRequest
	 * @return 204 No Content
	 */
	@DeleteMapping("/{provider}/unlink")
	public ResponseEntity<Void> unlink(
		@PathVariable String provider,
		HttpServletRequest request
	) {
		// 1. Provider 파싱
		OAuthProvider oauthProvider = oauthServiceFactory.parseProvider(provider);

		// 2. 현재 로그인한 사용자 TSID 추출
		String currentUserTsid = authService.getCurrentUserTsid(request);

		// 3. OAuth 계정 연동 해제
		oauthUserService.unlinkOAuthAccount(currentUserTsid, oauthProvider);

		return ResponseEntity.noContent().build();
	}
}