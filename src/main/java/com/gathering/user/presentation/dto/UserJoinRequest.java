package com.gathering.user.presentation.dto;

import com.gathering.common.annotation.AesEncrypted;
import com.gathering.user.domain.model.UsersEntity;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserJoinRequest {

	@Email
	@NotNull
	private final String email;
	@NotNull
	@AesEncrypted
	private final String password;
	@Nullable
	private final String nickname;
	@NotNull
	private final String name;
	private final String phoneNumber;

	public static UsersEntity toUsersEntity(UserJoinRequest request) {
		return UsersEntity.builder()
			.email(request.email)
			.nickname(request.nickname)
			.name(request.name)
			.phoneNumber(request.phoneNumber)
			.build();
	}
}
