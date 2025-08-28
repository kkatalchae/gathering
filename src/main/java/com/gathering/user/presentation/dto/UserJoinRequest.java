package com.gathering.user.presentation.dto;

import com.gathering.user.domain.model.UsersEntity;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
	@NotNull
	private final String name;
	@NotNull
	@Pattern(regexp = "^\\d+$", message = "전화번호는 숫자만 입력해주세요. 예: 01012345678")
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
