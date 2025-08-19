package com.gathering.user.application.service;

import org.springframework.stereotype.Service;

import com.gathering.user.domain.repository.UserSecurityRespository;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

	private final UsersRepository usersRepository;
	private final UserSecurityRespository userSecurityRespository;

	public void join(UserJoinRequest request) {

		usersRepository.save(UserJoinRequest.toUsersEntity(request));
		userSecurityRespository.save(UserJoinRequest.toUserSecurityEntity(request));

	}
}
