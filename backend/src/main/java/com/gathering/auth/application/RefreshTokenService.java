package com.gathering.auth.application;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gathering.common.adapter.RedisAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RefreshToken 관리 서비스 (Redis 기반)
 * - RefreshToken 저장/조회/삭제
 * - TTL 자동 관리
 * - JTI를 통한 멀티 디바이스 지원
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
	 * RefreshToken 저장 (멀티 디바이스 지원)
	 * @param tsid 사용자 고유 ID
	 * @param jti JWT ID (토큰 고유 ID)
	 * @param refreshToken 리프레시 토큰
	 */
	public void saveRefreshToken(String tsid, String jti, String refreshToken) {
		String key = createKey(tsid, jti);
		redisAdapter.set(key, refreshToken, Duration.ofSeconds(refreshTokenValidityInSeconds));
		log.info("RefreshToken 저장 완료: tsid={}, jti={}", tsid, jti);
	}

	/**
	 * RefreshToken 조회 및 검증
	 * @param tsid 사용자 고유 ID
	 * @param jti JWT ID (토큰 고유 ID)
	 * @param refreshToken 검증할 토큰
	 * @return 유효 여부
	 */
	public boolean validateRefreshToken(String tsid, String jti, String refreshToken) {
		String key = createKey(tsid, jti);
		return redisAdapter.get(key)
			.map(storedToken -> storedToken.equals(refreshToken))
			.orElse(false);
	}

	/**
	 * RefreshToken 삭제 (로그아웃 시 사용)
	 * @param tsid 사용자 고유 ID
	 * @param jti JWT ID (토큰 고유 ID)
	 */
	public void deleteRefreshToken(String tsid, String jti) {
		String key = createKey(tsid, jti);
		boolean deleted = redisAdapter.delete(key);
		log.info("RefreshToken 삭제: tsid={}, jti={}, deleted={}", tsid, jti, deleted);
	}

	/**
	 * 사용자의 모든 RefreshToken 삭제 (회원 탈퇴 시 사용)
	 * 멀티 디바이스 환경에서 해당 사용자의 모든 디바이스 토큰을 삭제
	 * @param tsid 사용자 고유 ID
	 * @return 삭제된 토큰 개수
	 */
	public long deleteAllRefreshTokensByTsid(String tsid) {
		String pattern = KEY_PREFIX + tsid + ":*";
		long deletedCount = redisAdapter.deleteByPattern(pattern);
		log.info("사용자의 모든 RefreshToken 삭제: tsid={}, deletedCount={}", tsid, deletedCount);
		return deletedCount;
	}

	/**
	 * Redis 키 생성 (멀티 디바이스 지원)
	 * @param tsid 사용자 고유 ID
	 * @param jti JWT ID (토큰 고유 ID)
	 * @return Redis 키
	 */
	private String createKey(String tsid, String jti) {
		return KEY_PREFIX + tsid + ":" + jti;
	}
}
