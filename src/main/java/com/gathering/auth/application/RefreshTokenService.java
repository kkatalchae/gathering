package com.gathering.auth.application;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gathering.auth.infra.RedisAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RefreshToken 관리 서비스 (Redis 기반)
 * - RefreshToken 저장/조회/삭제
 * - TTL 자동 관리
 * - Refresh Token Rotation 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RedisAdapter redisAdapter;

	@Value("${jwt.refresh-token-validity-in-seconds}")
	private long refreshTokenValidityInSeconds;

	private static final String KEY_PREFIX = "refresh_token:";

	/**
	 * RefreshToken 저장
	 * @param tsid 사용자 고유 ID
	 * @param refreshToken 리프레시 토큰
	 */
	public void saveRefreshToken(String tsid, String refreshToken) {
		String key = KEY_PREFIX + tsid;
		redisAdapter.set(key, refreshToken, Duration.ofSeconds(refreshTokenValidityInSeconds));
		log.debug("RefreshToken 저장 완료: {}", tsid);
	}

	/**
	 * RefreshToken 조회 및 검증
	 * @param tsid 사용자 고유 ID
	 * @param refreshToken 검증할 토큰
	 * @return 유효 여부
	 */
	public boolean validateRefreshToken(String tsid, String refreshToken) {
		String key = KEY_PREFIX + tsid;
		return redisAdapter.get(key)
			.map(storedToken -> storedToken.equals(refreshToken))
			.orElse(false);
	}

	/**
	 * RefreshToken 삭제 (로그아웃 시 사용)
	 * @param tsid 사용자 고유 ID
	 */
	public void deleteRefreshToken(String tsid) {
		String key = KEY_PREFIX + tsid;
		redisAdapter.delete(key);
	}
}