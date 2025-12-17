package com.gathering.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.auth.infra.RedisAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth state 파라미터 관리 서비스
 * CSRF 공격 방지를 위해 state와 session 정보를 Redis에 저장/검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthStateService {

	private static final String STATE_KEY_PREFIX = "oauth_state:";
	private static final Duration STATE_TTL = Duration.ofMinutes(5);

	private final RedisAdapter redisAdapter;
	private final ObjectMapper objectMapper;

	/**
	 * state 생성 및 사용자 session 정보 저장
	 * CSRF 방어를 위해 state와 session(refreshToken JTI)을 함께 저장
	 *
	 * @param userTsid 현재 로그인한 사용자 TSID (비로그인 상태면 null)
	 * @param refreshTokenJti 현재 로그인한 사용자의 refreshToken JTI (비로그인 상태면 null)
	 * @return 생성된 state (UUID)
	 */
	public String generateState(String userTsid, String refreshTokenJti) {
		String state = UUID.randomUUID().toString();
		String key = STATE_KEY_PREFIX + state;

		OAuthStateData data = OAuthStateData.builder()
			.createdAt(Instant.now())
			.userTsid(userTsid)
			.refreshTokenJti(refreshTokenJti)
			.build();

		try {
			String jsonValue = objectMapper.writeValueAsString(data);
			redisAdapter.set(key, jsonValue, STATE_TTL);
			log.debug("OAuth state 생성: state={}, userTsid={}", state, userTsid);
		} catch (JsonProcessingException e) {
			log.error("OAuth state 저장 실패: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to save OAuth state", e);
		}

		return state;
	}

	/**
	 * state 검증 및 session 일관성 확인
	 * state 존재 여부와 함께 OAuth 시작 사용자와 완료 사용자가 동일한지 검증
	 *
	 * @param state 검증할 state
	 * @param currentUserTsid 현재 요청한 사용자 TSID (비로그인 상태면 null)
	 * @param currentRefreshTokenJti 현재 요청한 사용자의 refreshToken JTI (비로그인 상태면 null)
	 * @return 검증 결과
	 */
	public ValidationResult validateAndConsumeState(String state, String currentUserTsid, String currentRefreshTokenJti) {
		String key = STATE_KEY_PREFIX + state;
		Optional<String> valueOpt = redisAdapter.get(key);

		if (valueOpt.isEmpty()) {
			log.warn("유효하지 않은 state: {}", state);
			return ValidationResult.INVALID_STATE;
		}

		try {
			OAuthStateData savedData = objectMapper.readValue(valueOpt.get(), OAuthStateData.class);

			// state 삭제 (재사용 방지)
			redisAdapter.delete(key);

			// session 일관성 검증
			if (savedData.getUserTsid() != null) {
				// 로그인 상태로 시작했으면 현재도 로그인 상태여야 하고, 동일한 사용자여야 함
				if (currentUserTsid == null || !savedData.getUserTsid().equals(currentUserTsid)) {
					log.warn("OAuth session 불일치: saved={}, current={}", savedData.getUserTsid(), currentUserTsid);
					return ValidationResult.SESSION_MISMATCH;
				}

				if (savedData.getRefreshTokenJti() != null &&
					!savedData.getRefreshTokenJti().equals(currentRefreshTokenJti)) {
					log.warn("OAuth refreshToken JTI 불일치: saved={}, current={}",
						savedData.getRefreshTokenJti(), currentRefreshTokenJti);
					return ValidationResult.SESSION_MISMATCH;
				}
			}

			log.debug("OAuth state 검증 성공: state={}, userTsid={}", state, currentUserTsid);
			return ValidationResult.VALID;

		} catch (JsonProcessingException e) {
			log.error("OAuth state 파싱 실패: {}", e.getMessage(), e);
			return ValidationResult.INVALID_STATE;
		}
	}

	/**
	 * OAuth state 검증 결과
	 */
	public enum ValidationResult {
		VALID,
		INVALID_STATE,
		SESSION_MISMATCH
	}

	/**
	 * Redis에 저장되는 OAuth state 데이터
	 */
	@lombok.Data
	@lombok.Builder
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	private static class OAuthStateData {

		private Instant createdAt;
		private String userTsid;
		private String refreshTokenJti;
	}
}