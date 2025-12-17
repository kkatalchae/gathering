package com.gathering.user.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.presentation.dto.OAuthProviderResponse;
import com.gathering.user.application.OAuthUserService;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.presentation.dto.ChangePasswordRequest;
import com.gathering.user.presentation.dto.MyInfoResponse;
import com.gathering.user.presentation.dto.UpdateMyInfoRequest;
import com.gathering.user.presentation.dto.UserInfoResponse;
import com.gathering.user.presentation.dto.UserJoinRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UsersController {

	private final UserService userService;
	private final AuthService authService;
	private final OAuthUserService oauthUserService;

	@PostMapping("/join")
	public ResponseEntity<Void> join(@Valid @RequestBody UserJoinRequest request) {

		userService.join(request);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{tsid}")
	public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable String tsid) {
		UsersEntity user = userService.getUserInfo(tsid);
		UserInfoResponse response = UserInfoResponse.from(user);
		return ResponseEntity.ok(response);
	}

	/**
	 * 현재 로그인한 사용자의 상세 정보 조회
	 */
	@GetMapping("/me")
	public ResponseEntity<MyInfoResponse> getMyInfo(HttpServletRequest request) {
		String tsid = authService.getCurrentUserTsid(request);
		MyInfoResponse response = userService.getMyInfo(tsid);
		return ResponseEntity.ok(response);
	}

	/**
	 * 내 정보 수정
	 */
	@PatchMapping("/me")
	public ResponseEntity<MyInfoResponse> updateMyInfo(
		HttpServletRequest request,
		@Valid @RequestBody UpdateMyInfoRequest updateRequest) {
		String tsid = authService.getCurrentUserTsid(request);
		MyInfoResponse response = userService.updateMyInfo(tsid, updateRequest);
		return ResponseEntity.ok(response);
	}

	/**
	 * 비밀번호 변경
	 */
	@PutMapping("/me/password")
	public ResponseEntity<Void> changePassword(
		HttpServletRequest request,
		@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
		String tsid = authService.getCurrentUserTsid(request);
		userService.changePassword(tsid, changePasswordRequest);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 현재 로그인한 사용자의 연동된 OAuth 제공자 목록 조회
	 */
	@GetMapping("/me/oauth-providers")
	public ResponseEntity<List<OAuthProviderResponse>> getMyOAuthProviders(HttpServletRequest request) {
		String tsid = authService.getCurrentUserTsid(request);
		List<OAuthProviderResponse> providers = oauthUserService.getLinkedProviders(tsid);
		return ResponseEntity.ok(providers);
	}

}

