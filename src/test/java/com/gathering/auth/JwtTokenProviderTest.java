package com.gathering.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.gathering.auth.infra.JwtTokenProvider;

import io.jsonwebtoken.Claims;

/**
 * JwtTokenProvider 단위 테스트
 */
class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;
	private final String secretKey = "test-secret-key-for-jwt-token-generation-must-be-long-enough";
	private final long accessTokenValidityInSeconds = 3600L; // 1시간
	private final long refreshTokenValidityInSeconds = 604800L; // 7일

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider();
		ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secretKey);
		ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityInSeconds",
			accessTokenValidityInSeconds);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidityInSeconds",
			refreshTokenValidityInSeconds);
		jwtTokenProvider.init();
	}

	@Test
	@DisplayName("액세스 토큰 생성 성공")
	void createAccessToken_success() {
		// given
		String email = "test@example.com";

		// when
		String token = jwtTokenProvider.createAccessToken(email);

		// then
		assertThat(token).isNotNull();
		assertThat(token).isNotEmpty();
	}

	@Test
	@DisplayName("리프레시 토큰 생성 성공")
	void createRefreshToken_success() {
		// given
		String email = "test@example.com";

		// when
		String token = jwtTokenProvider.createRefreshToken(email);

		// then
		assertThat(token).isNotNull();
		assertThat(token).isNotEmpty();
	}

	@Test
	@DisplayName("토큰에서 이메일 추출 성공")
	void getEmailFromToken_success() {
		// given
		String email = "test@example.com";
		String token = jwtTokenProvider.createAccessToken(email);

		// when
		String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

		// then
		assertThat(extractedEmail).isEqualTo(email);
	}

	@Test
	@DisplayName("유효한 토큰 검증 성공")
	void validateToken_success() {
		// given
		String email = "test@example.com";
		String token = jwtTokenProvider.createAccessToken(email);

		// when
		boolean isValid = jwtTokenProvider.validateToken(token);

		// then
		assertThat(isValid).isTrue();
	}

	@Test
	@DisplayName("유효하지 않은 토큰 검증 실패")
	void validateToken_fail_invalid_token() {
		// given
		String invalidToken = "invalid.token.value";

		// when
		boolean isValid = jwtTokenProvider.validateToken(invalidToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("만료된 토큰 검증 실패")
	void validateToken_fail_expired_token() {
		// given
		JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
		ReflectionTestUtils.setField(expiredTokenProvider, "secretKey", secretKey);
		ReflectionTestUtils.setField(expiredTokenProvider, "accessTokenValidityInSeconds", -1L); // 이미 만료
		ReflectionTestUtils.setField(expiredTokenProvider, "refreshTokenValidityInSeconds",
			refreshTokenValidityInSeconds);
		expiredTokenProvider.init();

		String email = "test@example.com";
		String expiredToken = expiredTokenProvider.createAccessToken(email);

		// when
		boolean isValid = jwtTokenProvider.validateToken(expiredToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("토큰 만료 시간 확인")
	void getExpirationFromToken_success() {
		// given
		String email = "test@example.com";
		String token = jwtTokenProvider.createAccessToken(email);

		// when
		Instant expiration = jwtTokenProvider.getExpirationFromToken(token);

		// then
		assertThat(expiration).isNotNull();
		assertThat(expiration).isAfter(Instant.now());
		// 만료 시간이 대략 1시간 후인지 확인 (오차 범위 ±10초)
		long expectedExpirationSeconds = Instant.now().plus(accessTokenValidityInSeconds, ChronoUnit.SECONDS)
			.getEpochSecond();
		long actualExpirationSeconds = expiration.getEpochSecond();
		assertThat(Math.abs(expectedExpirationSeconds - actualExpirationSeconds)).isLessThan(10);
	}

	@Test
	@DisplayName("토큰에서 모든 Claims 추출 성공")
	void getAllClaimsFromToken_success() {
		// given
		String email = "test@example.com";
		String token = jwtTokenProvider.createAccessToken(email);

		// when
		Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

		// then
		assertThat(claims).isNotNull();
		assertThat(claims.getSubject()).isEqualTo(email);
		assertThat(claims.getExpiration()).isNotNull();
		assertThat(claims.getIssuedAt()).isNotNull();
	}

	@Test
	@DisplayName("액세스 토큰과 리프레시 토큰의 만료 시간이 다름")
	void accessToken_and_refreshToken_have_different_expiration() {
		// given
		String email = "test@example.com";

		// when
		String accessToken = jwtTokenProvider.createAccessToken(email);
		String refreshToken = jwtTokenProvider.createRefreshToken(email);

		Instant accessTokenExpiration = jwtTokenProvider.getExpirationFromToken(accessToken);
		Instant refreshTokenExpiration = jwtTokenProvider.getExpirationFromToken(refreshToken);

		// then
		assertThat(refreshTokenExpiration).isAfter(accessTokenExpiration);
	}
}