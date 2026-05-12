package com.gathering.common.adapter;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis 어댑터
 * RedisTemplate의 복잡한 API를 간단하고 안전한 API로 변환
 */
@Component
@RequiredArgsConstructor
public class RedisAdapter {

	private final RedisTemplate<String, String> redisTemplate;

	/**
	 * 값 저장 (TTL 포함)
	 * @param key 키
	 * @param value 값
	 * @param duration 만료 시간
	 */
	public void set(String key, String value, Duration duration) {
		redisTemplate.opsForValue().set(key, value, duration);
	}

	/**
	 * 값 조회
	 * @param key 키
	 * @return Optional로 감싼 값
	 */
	public Optional<String> get(String key) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key));
	}

	/**
	 * 값 삭제
	 * @param key 키
	 * @return 삭제 성공 여부
	 */
	public boolean delete(String key) {
		return Boolean.TRUE.equals(redisTemplate.delete(key));
	}

	/**
	 * 패턴과 일치하는 모든 키 삭제
	 * @param pattern 키 패턴 (예: "refresh_token:123456:*")
	 * @return 삭제된 키 개수
	 */
	public long deleteByPattern(String pattern) {
		Set<String> keys = redisTemplate.keys(pattern);
		if (keys == null || keys.isEmpty()) {
			return 0;
		}
		Long deletedCount = redisTemplate.delete(keys);
		return deletedCount != null ? deletedCount : 0;
	}
}
