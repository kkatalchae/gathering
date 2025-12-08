package com.gathering.user.presentation.dto;

import com.gathering.user.domain.model.UsersEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 조회 응답 DTO
 * 민감한 정보(이메일, 전화번호 등)를 제외하고 공개 가능한 정보만 포함
 */
@Getter
@Builder
public class UserInfoResponse {

	private String nickname;
	private String name;
	private String profileImageUrl;

	public static UserInfoResponse from(UsersEntity user) {
		return UserInfoResponse.builder()
			.nickname(user.getNickname())
			.name(user.getName())
			.profileImageUrl(user.getProfileImageUrl())
			.build();
	}
}