package com.gathering.user.presentation.dto;

import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserJoinRequest {

	@Email
	@NotNull
	private final String email;
	@NotNull
	private final String password;
	@Nullable
	private final String nickname;

	public static UsersEntity toUsersEntity(UserJoinRequest request) {
		return UsersEntity.builder()
			.email(request.email)
			.nickname(request.nickname)
			.build();
	}

	public static UserSecurityEntity toUserSecurityEntity(UserJoinRequest request) {
		return UserSecurityEntity.builder()
			.passwordHash(request.password)
			.build();
	}

}
