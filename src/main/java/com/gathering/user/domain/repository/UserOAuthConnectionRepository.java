package com.gathering.user.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.UserOAuthConnectionEntity;

@Repository
public interface UserOAuthConnectionRepository extends JpaRepository<UserOAuthConnectionEntity, String> {

	/**
	 * 특정 OAuth 제공자와 provider ID로 연동 정보 조회
	 */
	Optional<UserOAuthConnectionEntity> findByProviderAndProviderId(
		OAuthProvider provider,
		String providerId
	);

	/**
	 * 특정 사용자의 특정 OAuth 제공자 연동 정보 조회
	 */
	Optional<UserOAuthConnectionEntity> findByUserTsidAndProvider(String userTsid, OAuthProvider provider);

	/**
	 * 특정 사용자의 모든 소셜 연동 정보 조회
	 */
	List<UserOAuthConnectionEntity> findAllByUserTsid(String userTsid);

	/**
	 * 특정 사용자의 모든 소셜 연동 정보 삭제 (회원 탈퇴 시 사용)
	 */
	void deleteByUserTsid(String userTsid);

	long countByUserTsidAndProviderNot(String userTsid, OAuthProvider provider);
}
