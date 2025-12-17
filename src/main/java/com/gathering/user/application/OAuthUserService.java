package com.gathering.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.auth.domain.OAuthUserInfo;
import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.OAuthProviderEntity;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.OAuthProviderRepository;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth 사용자 관리 서비스
 * OAuth 로그인을 통한 회원가입, 계정 연동/해제 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUserService {

	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;
	private final OAuthProviderRepository oauthProviderRepository;

	/**
	 * OAuth 사용자 찾기 또는 신규 생성
	 * 이메일 중복 시 예외 발생 (기존 계정 로그인 후 연동 유도)
	 *
	 * @param oauthUserInfo OAuth 제공자에서 받은 사용자 정보
	 * @return 사용자 엔티티
	 * @throws BusinessException EMAIL_ALREADY_REGISTERED - 이메일이 이미 존재하는 경우
	 */
	@Transactional
	public UsersEntity findOrCreateUser(OAuthUserInfo oauthUserInfo) {
		// 1. OAuth 제공자 정보로 기존 사용자 찾기
		return oauthProviderRepository
			.findByProviderAndProviderUserId(oauthUserInfo.getProvider(), oauthUserInfo.getProviderId())
			.map(OAuthProviderEntity::getUser)
			.orElseGet(() -> createNewOAuthUser(oauthUserInfo));
	}

	/**
	 * 신규 OAuth 사용자 생성
	 * 이메일이 이미 존재하면 예외 발생
	 *
	 * @param oauthUserInfo OAuth 사용자 정보
	 * @return 생성된 사용자
	 */
	private UsersEntity createNewOAuthUser(OAuthUserInfo oauthUserInfo) {
		// 이메일 중복 체크
		if (usersRepository.findByEmail(oauthUserInfo.getEmail()).isPresent()) {
			log.warn("OAuth 회원가입 시도 중 이메일 중복 발견: {}", oauthUserInfo.getEmail());
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}

		// UsersEntity 생성
		UsersEntity user = UsersEntity.builder()
			.email(oauthUserInfo.getEmail())
			.name(oauthUserInfo.getName())
			.emailVerified(oauthUserInfo.getEmailVerified())
			.profileImageUrl(oauthUserInfo.getProfileImageUrl())
			.build();

		UsersEntity savedUser = usersRepository.save(user);
		log.info("신규 OAuth 사용자 생성: tsid={}, email={}, provider={}",
			savedUser.getTsid(), savedUser.getEmail(), oauthUserInfo.getProvider());

		// UserSecurityEntity 생성 (비밀번호 없음)
		UserSecurityEntity security = UserSecurityEntity.of(savedUser.getTsid(), null);
		userSecurityRepository.save(security);

		// OAuthProviderEntity 생성
		OAuthProviderEntity oauthProvider = OAuthProviderEntity.of(
			savedUser.getTsid(),
			oauthUserInfo.getProvider(),
			oauthUserInfo.getProviderId(),
			oauthUserInfo.getEmail(),
			oauthUserInfo.getProfileImageUrl()
		);
		oauthProviderRepository.save(oauthProvider);

		return savedUser;
	}

	/**
	 * 기존 사용자에게 OAuth 계정 연동
	 * 로그인한 사용자만 호출 가능
	 *
	 * @param userTsid 사용자 TSID
	 * @param oauthUserInfo OAuth 사용자 정보
	 * @throws BusinessException OAUTH_ACCOUNT_ALREADY_LINKED - 이미 다른 사용자에게 연동된 경우
	 */
	@Transactional
	public void linkOAuthAccount(String userTsid, OAuthUserInfo oauthUserInfo) {
		// 1. 사용자 존재 확인
		UsersEntity user = usersRepository.findById(userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 2. 동일 OAuth 계정이 이미 다른 사용자에게 연동되어 있는지 확인
		oauthProviderRepository
			.findByProviderAndProviderUserId(oauthUserInfo.getProvider(), oauthUserInfo.getProviderId())
			.ifPresent(existing -> {
				if (!existing.getUserTsid().equals(userTsid)) {
					log.warn("이미 다른 사용자에게 연동된 OAuth 계정: provider={}, providerId={}, existingUserTsid={}",
						oauthUserInfo.getProvider(), oauthUserInfo.getProviderId(), existing.getUserTsid());
					throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
				}
			});

		// 3. 이미 현재 사용자에게 연동되어 있는지 확인 (중복 연동 방지)
		if (oauthProviderRepository.findByUserTsidAndProvider(userTsid, oauthUserInfo.getProvider()).isPresent()) {
			log.info("이미 연동된 OAuth 계정 (재연동 시도): userTsid={}, provider={}",
				userTsid, oauthUserInfo.getProvider());
			return;
		}

		// 4. OAuth 계정 연동
		OAuthProviderEntity oauthProvider = OAuthProviderEntity.of(
			userTsid,
			oauthUserInfo.getProvider(),
			oauthUserInfo.getProviderId(),
			oauthUserInfo.getEmail(),
			oauthUserInfo.getProfileImageUrl()
		);
		oauthProviderRepository.save(oauthProvider);

		log.info("OAuth 계정 연동 완료: userTsid={}, provider={}", userTsid, oauthUserInfo.getProvider());
	}

	/**
	 * OAuth 계정 연동 해제
	 * 마지막 인증 수단인 경우 해제 불가
	 *
	 * @param userTsid 사용자 TSID
	 * @param provider OAuth 제공자
	 * @throws BusinessException OAUTH_PROVIDER_NOT_LINKED - 연동되지 않은 제공자
	 * @throws BusinessException OAUTH_CANNOT_UNLINK_LAST_AUTH - 마지막 인증 수단
	 */
	@Transactional
	public void unlinkOAuthAccount(String userTsid, OAuthProvider provider) {
		// 1. 사용자 존재 확인
		UsersEntity user = usersRepository.findById(userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 2. OAuth 계정 연동 확인
		OAuthProviderEntity oauthProvider = oauthProviderRepository
			.findByUserTsidAndProvider(userTsid, provider)
			.orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_LINKED));

		// 3. 해제 가능 여부 확인
		if (!canUnlinkOAuth(userTsid, provider)) {
			log.warn("마지막 인증 수단 해제 시도: userTsid={}, provider={}", userTsid, provider);
			throw new BusinessException(ErrorCode.OAUTH_CANNOT_UNLINK_LAST_AUTH);
		}

		// 4. OAuth 계정 연동 해제
		oauthProviderRepository.delete(oauthProvider);
		log.info("OAuth 계정 연동 해제 완료: userTsid={}, provider={}", userTsid, provider);
	}

	/**
	 * OAuth 계정 연동 해제 가능 여부 확인
	 * 비밀번호가 설정되어 있거나 다른 OAuth 계정이 연동되어 있어야 함
	 *
	 * @param userTsid 사용자 TSID
	 * @param provider 해제하려는 OAuth 제공자
	 * @return 해제 가능 여부
	 */
	public boolean canUnlinkOAuth(String userTsid, OAuthProvider provider) {
		// 1. 비밀번호가 설정되어 있는지 확인
		UserSecurityEntity security = userSecurityRepository.findById(userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (security.getPasswordHash() != null) {
			return true;
		}

		// 2. 다른 OAuth 계정이 연동되어 있는지 확인
		long oauthCount = oauthProviderRepository.countByUserTsid(userTsid);
		return oauthCount > 1;
	}
}