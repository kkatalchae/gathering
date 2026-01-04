package com.gathering.user.presentation.dto;

import java.time.Instant;
import java.util.List;

import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 내 정보 조회 응답 DTO
 * /users/me API에서 사용 - 현재 로그인한 사용자의 상세 정보
 * MeResponse를 대체하여 더 많은 정보를 포함
 */
@Getter
@Builder
public class MyInfoResponse {
	private String tsid;
	private String email;
	private String nickname;
	private String name;
	private String phoneNumber;
	private String profileImageUrl;
	private UserStatus status;
	private Instant createdAt;
	private Boolean hasPassword;
	private List<OAuthProvider> connectedProviders;

	public static MyInfoResponse from(UsersEntity user, Boolean hasPassword, List<OAuthProvider> connectedProviders) {
		return MyInfoResponse.builder()
			.tsid(user.getTsid())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.name(user.getName())
			.phoneNumber(user.getPhoneNumber())
			.profileImageUrl(user.getProfileImageUrl())
			.status(user.getStatus())
			.createdAt(user.getCreatedAt())
			.hasPassword(hasPassword)
			.connectedProviders(connectedProviders)
			.build();
	}
}