package com.gathering.user.presentation.dto;

import com.gathering.user.domain.model.UsersEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 내 정보 조회 응답 DTO
 * /users/me API에서 사용 - 현재 로그인한 사용자의 기본 정보
 */
@Getter
@Builder
public class MeResponse {
	private String tsid;

	public static MeResponse from(UsersEntity user) {
		return MeResponse.builder()
			.tsid(user.getTsid())
			.build();
	}
}