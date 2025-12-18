package com.gathering.user.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.ChangePasswordRequest;
import com.gathering.user.presentation.dto.MyInfoResponse;
import com.gathering.user.presentation.dto.UpdateMyInfoRequest;
import com.gathering.user.presentation.dto.UserJoinRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserValidator userValidator;

	/**
	 * 회원가입 처리
	 * Note: 비밀번호는 @AesEncrypted 어노테이션에 의해 DTO 바인딩 시점에 자동으로 복호화됨
	 */
	@Transactional
	public void join(UserJoinRequest request) {
		userValidator.validateForJoin(request);

		UsersEntity usersEntity = usersRepository.save(UserJoinRequest.toUsersEntity(request));
		// 비밀번호는 이미 복호화되어 있으므로 바로 BCrypt 인코딩
		UserSecurityEntity userSecurityEntity = UserSecurityEntity.of(
			usersEntity.getTsid(),
			passwordEncoder.encode(request.getPassword())
		);
		userSecurityRepository.save(userSecurityEntity);
	}

	/**
	 * 소셜 회원가입 처리
	 *
	 * @param oAuthUserInfo OAuth 제공자에서 받은 사용자 정보
	 * @return 생성된 사용자 엔티티
	 */
	public UsersEntity socialJoin(OAuthUserInfo oAuthUserInfo) {

		UsersEntity usersEntity = OAuthUserInfo.toUsersEntity(oAuthUserInfo);

		userValidator.validateForSocialJoin(usersEntity);

		usersRepository.save(usersEntity);

		UserSecurityEntity userSecurityEntity = UserSecurityEntity.of(usersEntity.getTsid(), null);

		userSecurityRepository.save(userSecurityEntity);

		return usersEntity;
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

	/**
	 * 현재 로그인한 사용자의 상세 정보 조회
	 *
	 * @param tsid 사용자 고유 ID
	 * @return 사용자 상세 정보 (email, phoneNumber 포함)
	 */
	public MyInfoResponse getMyInfo(String tsid) {
		UsersEntity user = getUserInfo(tsid);
		return MyInfoResponse.from(user);
	}

	/**
	 * 내 정보 수정
	 *
	 * @param tsid 사용자 고유 ID (JWT에서 추출)
	 * @param request 수정할 정보
	 * @return 수정된 사용자 정보
	 */
	@Transactional
	public MyInfoResponse updateMyInfo(String tsid, UpdateMyInfoRequest request) {
		// 1. request에서 값 추출
		String nickname = request.getNickname();
		String name = request.getName();
		String phoneNumber = request.getPhoneNumber();

		// 2. 사용자 조회 및 상태 검증
		UsersEntity user = getUserInfo(tsid);

		// 3. 이름 검증
		userValidator.validateName(name);

		// 4. 전화번호 검증 (변경 시에만 형식 및 중복 체크)
		if (phoneNumber != null) {
			userValidator.validatePhoneNumberFormat(phoneNumber);
			if (!phoneNumber.equals(user.getPhoneNumber())) {
				userValidator.validatePhoneNumberUnique(phoneNumber);
			}
		}

		// 5. 엔티티 업데이트 (JPA dirty checking으로 자동 UPDATE)
		user.updateProfile(nickname, name, phoneNumber);

		// 6. 업데이트된 정보 반환
		return MyInfoResponse.from(user);
	}

	/**
	 * 비밀번호 변경
	 *
	 * @param tsid 사용자 고유 ID (JWT에서 추출)
	 * @param request 현재 비밀번호 및 새 비밀번호
	 */
	@Transactional
	public void changePassword(String tsid, ChangePasswordRequest request) {
		// request에서 값 추출
		String currentPassword = request.getCurrentPassword();
		String newPassword = request.getNewPassword();

		// 보안 정보 조회
		UserSecurityEntity security = userSecurityRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 현재 비밀번호 검증 (AES 복호화는 이미 완료됨)
		if (!passwordEncoder.matches(currentPassword, security.getPasswordHash())) {
			throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
		}

		// 새 비밀번호 정책 검증
		userValidator.validatePasswordFormat(newPassword);

		// 비밀번호 업데이트 (JPA dirty checking으로 자동 UPDATE)
		security.updatePassword(passwordEncoder.encode(newPassword));
	}
}
