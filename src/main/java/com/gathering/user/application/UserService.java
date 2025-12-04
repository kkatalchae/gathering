package com.gathering.user.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.UserJoinRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserJoinValidator userJoinValidator;

	/**
	 * 회원가입 처리
	 *
	 * Note: 비밀번호는 @AesEncrypted 어노테이션에 의해 DTO 바인딩 시점에 자동으로 복호화됨
	 */
	@Transactional
	public void join(UserJoinRequest request) {
		userJoinValidator.validateUser(request);

		UsersEntity usersEntity = usersRepository.save(UserJoinRequest.toUsersEntity(request));
		// 비밀번호는 이미 복호화되어 있으므로 바로 BCrypt 인코딩
		UserSecurityEntity userSecurityEntity = UserSecurityEntity.of(
			usersEntity.getTsid(),
			passwordEncoder.encode(request.getPassword())
		);
		userSecurityRepository.save(userSecurityEntity);
	}

	/**
	 * 사용자 정보 조회
	 *
	 * @param tsid 사용자 고유 ID
	 * @return 사용자 엔티티
	 * @throws BusinessException 사용자가 존재하지 않거나 삭제/정지된 경우
	 */
	public UsersEntity getUserInfo(String tsid) {
		UsersEntity user = usersRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (user.getStatus() == UserStatus.DELETED) {
			throw new BusinessException(ErrorCode.USER_DELETED);
		}

		if (user.getStatus() == UserStatus.BANNED) {
			throw new BusinessException(ErrorCode.USER_BANNED);
		}

		return user;
	}
}
