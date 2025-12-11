package com.gathering.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.auth.infra.JwtTokenProvider;

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
		String tsid = "1234567890123";

		// when
		String token = jwtTokenProvider.createAccessToken(tsid);

		// then
		assertThat(token).isNotNull();
		assertThat(token).isNotEmpty();
	}

	@Test
	@DisplayName("리프레시 토큰 생성 성공")
	void createRefreshToken_success() {
		// given
		String tsid = "1234567890123";

		// when
		String token = jwtTokenProvider.createRefreshToken(tsid);

		// then
		assertThat(token).isNotNull();
		assertThat(token).isNotEmpty();
	}

	@Test
	@DisplayName("토큰에서 TSID 추출 성공")
	void getTsidFromToken_success() {
		// given
		String tsid = "1234567890123";
		String token = jwtTokenProvider.createAccessToken(tsid);

		// when
		String extractedTsid = jwtTokenProvider.getTsidFromToken(token);

		// then
		assertThat(extractedTsid).isEqualTo(tsid);
	}

	@Test
	@DisplayName("토큰 만료 시간 확인")
	void getExpirationFromToken_success() {
		// given
		String tsid = "1234567890123";
		String token = jwtTokenProvider.createAccessToken(tsid);

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

	@Nested
	@DisplayName("JTI 추출")
	class JtiExtraction {

		@Test
		@DisplayName("리프레시 토큰을 생성하면 JTI가 포함된다")
		void createRefreshToken_containsJti() {
			// given
			String tsid = "1234567890123";

			// when
			String refreshToken = jwtTokenProvider.createRefreshToken(tsid);
			String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

			// then
			assertThat(jti).isNotBlank().isNotEmpty();
		}

		@Test
		@DisplayName("액세스 토큰을 생성하면 JTI가 포함되지 않는다")
		void createAccessToken_doesNotContainJti() {
			// given
			String tsid = "1234567890123";

			// when
			String accessToken = jwtTokenProvider.createAccessToken(tsid);
			String jti = jwtTokenProvider.getJtiFromToken(accessToken);

			// then
			assertThat(jti).isNull();
		}
	}

	@Nested
	@DisplayName("액세스 토큰 검증")
	class AccessTokenValidation {

		@Test
		@DisplayName("유효한 액세스 토큰을 검증하면 예외가 발생하지 않는다")
		void validateAccessToken_validToken_success() {
			// given
			String tsid = "1234567890123";
			String accessToken = jwtTokenProvider.createAccessToken(tsid);

			// when & then
			assertThatCode(() -> jwtTokenProvider.validateAccessToken(accessToken))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("만료된 액세스 토큰을 검증하면 ACCESS_TOKEN_EXPIRED 예외가 발생한다")
		void validateAccessToken_expiredToken_throwsException() {
			// given
			String tsid = "1234567890123";
			// 만료된 토큰 생성 (유효기간을 음수로 설정)
			JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
			ReflectionTestUtils.setField(expiredTokenProvider, "secretKey", secretKey);
			ReflectionTestUtils.setField(expiredTokenProvider, "accessTokenValidityInSeconds", -1L);
			ReflectionTestUtils.setField(expiredTokenProvider, "refreshTokenValidityInSeconds",
				refreshTokenValidityInSeconds);
			expiredTokenProvider.init();
			String expiredToken = expiredTokenProvider.createAccessToken(tsid);

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(expiredToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_EXPIRED);
		}

		@Test
		@DisplayName("잘못된 서명의 액세스 토큰을 검증하면 ACCESS_TOKEN_MALFORMED 예외가 발생한다")
		void validateAccessToken_invalidSignature_throwsException() {
			// given
			String tsid = "1234567890123";
			// 다른 시크릿 키로 토큰 생성
			JwtTokenProvider otherProvider = new JwtTokenProvider();
			ReflectionTestUtils.setField(otherProvider, "secretKey",
				"different-secret-key-for-testing-invalid-signature-must-be-long");
			ReflectionTestUtils.setField(otherProvider, "accessTokenValidityInSeconds",
				accessTokenValidityInSeconds);
			ReflectionTestUtils.setField(otherProvider, "refreshTokenValidityInSeconds",
				refreshTokenValidityInSeconds);
			otherProvider.init();
			String invalidToken = otherProvider.createAccessToken(tsid);

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(invalidToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_MALFORMED);
		}

		@Test
		@DisplayName("잘못된 형식의 액세스 토큰을 검증하면 ACCESS_TOKEN_MALFORMED 예외가 발생한다")
		void validateAccessToken_malformedToken_throwsException() {
			// given
			String malformedToken = "invalid-token-format";

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(malformedToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_MALFORMED);
		}
	}

	@Nested
	@DisplayName("리프레시 토큰 검증")
	class RefreshTokenValidation {

		@Test
		@DisplayName("유효한 리프레시 토큰을 검증하면 예외가 발생하지 않는다")
		void validateRefreshToken_validToken_success() {
			// given
			String tsid = "1234567890123";
			String refreshToken = jwtTokenProvider.createRefreshToken(tsid);

			// when & then
			assertThatCode(() -> jwtTokenProvider.validateRefreshToken(refreshToken))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("만료된 리프레시 토큰을 검증하면 REFRESH_TOKEN_EXPIRED 예외가 발생한다")
		void validateRefreshToken_expiredToken_throwsException() {
			// given
			String tsid = "1234567890123";
			// 만료된 토큰 생성 (유효기간을 음수로 설정)
			JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
			ReflectionTestUtils.setField(expiredTokenProvider, "secretKey", secretKey);
			ReflectionTestUtils.setField(expiredTokenProvider, "accessTokenValidityInSeconds",
				accessTokenValidityInSeconds);
			ReflectionTestUtils.setField(expiredTokenProvider, "refreshTokenValidityInSeconds", -1L);
			expiredTokenProvider.init();
			String expiredToken = expiredTokenProvider.createRefreshToken(tsid);

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(expiredToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		@Test
		@DisplayName("잘못된 서명의 리프레시 토큰을 검증하면 REFRESH_TOKEN_MALFORMED 예외가 발생한다")
		void validateRefreshToken_invalidSignature_throwsException() {
			// given
			String tsid = "1234567890123";
			// 다른 시크릿 키로 토큰 생성
			JwtTokenProvider otherProvider = new JwtTokenProvider();
			ReflectionTestUtils.setField(otherProvider, "secretKey",
				"different-secret-key-for-testing-invalid-signature-must-be-long");
			ReflectionTestUtils.setField(otherProvider, "accessTokenValidityInSeconds",
				accessTokenValidityInSeconds);
			ReflectionTestUtils.setField(otherProvider, "refreshTokenValidityInSeconds",
				refreshTokenValidityInSeconds);
			otherProvider.init();
			String invalidToken = otherProvider.createRefreshToken(tsid);

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(invalidToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_MALFORMED);
		}

		@Test
		@DisplayName("잘못된 형식의 리프레시 토큰을 검증하면 REFRESH_TOKEN_MALFORMED 예외가 발생한다")
		void validateRefreshToken_malformedToken_throwsException() {
			// given
			String malformedToken = "invalid-token-format";

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(malformedToken))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_MALFORMED);
		}
	}

}
