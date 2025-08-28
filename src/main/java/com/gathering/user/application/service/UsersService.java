package com.gathering.user.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;
import com.gathering.util.CryptoUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${crypto.aes.key}")
	private String aesKey;

	private String getPasswordHash(String encryptedPassword) {
		String decryptedPassword = CryptoUtil.decryptAES(encryptedPassword, aesKey);
		return passwordEncoder.encode(decryptedPassword);
	}

	@Transactional
	public void join(UserJoinRequest request) {
		UsersEntity usersEntity = usersRepository.save(UserJoinRequest.toUsersEntity(request));
		UserSecurityEntity userSecurityEntity = UserSecurityEntity.of(usersEntity.getTsid(),
			getPasswordHash(request.getPassword()));
		userSecurityRepository.save(userSecurityEntity);
	}
}
