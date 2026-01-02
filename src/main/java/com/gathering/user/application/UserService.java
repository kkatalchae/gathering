package com.gathering.user.application;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gathering.auth.application.RefreshTokenService;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.UserOAuthConnectionEntity;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.ChangePasswordRequest;
import com.gathering.user.presentation.dto.MyInfoResponse;
import com.gathering.user.presentation.dto.SetPasswordRequest;
import com.gathering.user.presentation.dto.UpdateMyInfoRequest;
import com.gathering.user.presentation.dto.UserJoinRequest;
import com.gathering.user.presentation.dto.WithdrawRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;
	private final UserOAuthConnectionRepository oauthConnectionRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserValidator userValidator;
	private final RefreshTokenService refreshTokenService;

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
	 * @throws BusinessException 사용자가 존재않은 경우
	 */
	public UsersEntity getUsersEntityByTsid(String tsid) {
		return usersRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 현재 로그인한 사용자의 상세 정보 조회
	 *
	 * @param tsid 사용자 고유 ID
	 * @return 사용자 상세 정보 (email, phoneNumber 포함)
	 */
	public MyInfoResponse getMyInfo(String tsid) {
		UsersEntity user = getUsersEntityByTsid(tsid);
		return buildMyInfoResponse(user);
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
		UsersEntity user = getUsersEntityByTsid(tsid);

		// 3. 이름 검증
		userValidator.validateName(name);

		// 4. 전화번호 형식 검증
		if (phoneNumber != null) {
			userValidator.validatePhoneNumberFormat(phoneNumber);
		}

		// 5. 엔티티 업데이트 (JPA dirty checking으로 자동 UPDATE)
		user.updateProfile(nickname, name, phoneNumber);

		// 6. 업데이트된 정보 반환
		return buildMyInfoResponse(user);
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

	/**
	 * 비밀번호 설정 (최초 설정 또는 재설정)
	 * 소셜 로그인 사용자가 비밀번호를 최초 설정하거나, 기존 비밀번호를 덮어쓸 수 있음
	 *
	 * @param tsid 사용자 고유 ID (JWT에서 추출)
	 * @param request 설정할 비밀번호
	 */
	@Transactional
	public void setPassword(String tsid, SetPasswordRequest request) {
		// 사용자 존재 확인
		getUsersEntityByTsid(tsid);

		// 보안 정보 조회
		UserSecurityEntity security = userSecurityRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 새 비밀번호 정책 검증
		userValidator.validatePasswordFormat(request.getPassword());

		// 비밀번호 업데이트 (기존 비밀번호 덮어쓰기 허용)
		security.updatePassword(passwordEncoder.encode(request.getPassword()));
	}

	/**
	 * 회원 탈퇴 (hard delete)
	 * 개인정보보호법에 따라 사용자 데이터를 완전히 삭제
	 *
	 * @param tsid 사용자 고유 ID
	 * @param request 회원 탈퇴 요청 (비밀번호 포함)
	 * @throws BusinessException 사용자가 존재하지 않거나 비밀번호가 일치하지 않는 경우
	 */
	@Transactional
	public void withdraw(String tsid, WithdrawRequest request) {
		// 1. 사용자 존재 확인
		getUsersEntityByTsid(tsid);

		// 2. 보안 정보 조회
		UserSecurityEntity security = userSecurityRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 3. 비밀번호 검증 (일반 회원가입 사용자만, 소셜 로그인 사용자는 password_hash가 null)
		if (security.getPasswordHash() != null && !passwordEncoder.matches(request.getPassword(),
			security.getPasswordHash())) {
			throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
		}

		deleteUsersByTsid(tsid);

		// 7. Redis에서 모든 refresh token 삭제 (멀티 디바이스 로그아웃)
		refreshTokenService.deleteAllRefreshTokensByTsid(tsid);
	}

	private void deleteUsersByTsid(String tsid) {
		// 소셜 연동 정보 삭제 (FK 제약으로 인해 먼저 삭제)
		oauthConnectionRepository.deleteByUserTsid(tsid);

		// user_security 테이블 삭제
		userSecurityRepository.deleteById(tsid);

		// users 테이블 삭제
		usersRepository.deleteById(tsid);
	}

	/**
	 * 사용자 엔티티로부터 MyInfoResponse 생성
	 * 비밀번호 설정 여부와 연동된 소셜 계정 목록 조회 로직을 캡슐화
	 *
	 * @param user 사용자 엔티티
	 * @return 사용자 상세 정보 응답
	 */
	private MyInfoResponse buildMyInfoResponse(UsersEntity user) {
		String tsid = user.getTsid();

		// passwordHash 존재 여부 확인
		UserSecurityEntity security = userSecurityRepository.findById(tsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Boolean hasPassword = security.getPasswordHash() != null;

		// 연동된 소셜 계정 목록 조회
		List<OAuthProvider> connectedProviders = oauthConnectionRepository.findAllByUserTsid(tsid)
			.stream()
			.map(UserOAuthConnectionEntity::getProvider)
			.toList();

		return MyInfoResponse.from(user, hasPassword, connectedProviders);
	}
}
