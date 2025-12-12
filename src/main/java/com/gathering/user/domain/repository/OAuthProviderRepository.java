package com.gathering.user.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gathering.user.domain.model.OAuthProvider;
import com.gathering.user.domain.model.OAuthProviderEntity;

/**
 * OAuth 제공자 리포지토리
 */
public interface OAuthProviderRepository extends JpaRepository<OAuthProviderEntity, Long> {

	/**
	 * OAuth 제공자와 제공자 사용자 ID로 조회
	 *
	 * @param provider OAuth 제공자
	 * @param providerUserId 제공자의 고유 사용자 ID
	 * @return OAuthProviderEntity
	 */
	Optional<OAuthProviderEntity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

	/**
	 * 사용자 TSID로 연동된 모든 OAuth 제공자 조회
	 *
	 * @param userTsid 사용자 TSID
	 * @return OAuth 제공자 목록
	 */
	List<OAuthProviderEntity> findAllByUserTsid(String userTsid);

	/**
	 * 사용자 TSID와 OAuth 제공자로 조회
	 *
	 * @param userTsid 사용자 TSID
	 * @param provider OAuth 제공자
	 * @return OAuthProviderEntity
	 */
	Optional<OAuthProviderEntity> findByUserTsidAndProvider(String userTsid, OAuthProvider provider);

	/**
	 * 사용자 TSID로 연동된 OAuth 제공자 개수 조회
	 *
	 * @param userTsid 사용자 TSID
	 * @return 연동된 OAuth 제공자 개수
	 */
	long countByUserTsid(String userTsid);
}