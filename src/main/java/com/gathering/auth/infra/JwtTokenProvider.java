package com.gathering.auth.infra;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 생성 및 검증을 담당하는 Provider
 */
@Slf4j
@Component
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-token-validity-in-seconds}")
	private long accessTokenValidityInSeconds;

	@Value("${jwt.refresh-token-validity-in-seconds}")
	private long refreshTokenValidityInSeconds;

	private SecretKey key;

	@PostConstruct
	public void init() {
		this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 액세스 토큰 생성
	 */
	public String createAccessToken(String tsid) {
		Instant now = Instant.now();
		Instant expiryDate = now.plusSeconds(accessTokenValidityInSeconds);

		return Jwts.builder()
			.subject(tsid)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiryDate))
			.signWith(key)
			.compact();
	}

	/**
	 * 리프레시 토큰 생성 (JTI 포함)
	 * JTI를 통해 멀티 디바이스 지원
	 */
	public String createRefreshToken(String tsid) {
		Instant now = Instant.now();
		Instant expiryDate = now.plusSeconds(refreshTokenValidityInSeconds);
		String jti = UUID.randomUUID().toString();

		return Jwts.builder()
			.subject(tsid)
			.id(jti)  // JTI 추가
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiryDate))
			.signWith(key)
			.compact();
	}

	/**
	 * 토큰에서 TSID 추출
	 */
	public String getTsidFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		return claims.getSubject();
	}

	/**
	 * 토큰에서 JTI 추출 (리프레시 토큰의 고유 ID)
	 */
	public String getJtiFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		return claims.getId();
	}

	/**
	 * 토큰에서 만료 시간 추출
	 */
	public Instant getExpirationFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		return claims.getExpiration().toInstant();
	}

	/**
	 * 토큰에서 모든 Claims 추출
	 */
	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	/**
	 * 액세스 토큰 유효성 검증 (상세 예외 발생)
	 * 클라이언트가 적절한 에러 처리를 할 수 있도록 구체적인 ErrorCode 제공
	 *
	 * @param token 검증할 액세스 토큰
	 * @throws BusinessException 토큰이 유효하지 않은 경우
	 */
	public void validateAccessToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
		} catch (ExpiredJwtException e) {
			log.debug("액세스 토큰 만료: {}", e.getMessage());
			throw new BusinessException(ErrorCode.ACCESS_TOKEN_EXPIRED, e);
		} catch (SecurityException | MalformedJwtException e) {
			log.error("액세스 토큰 서명 또는 형식 오류: {}", e.getMessage());
			throw new BusinessException(ErrorCode.ACCESS_TOKEN_MALFORMED, e);
		} catch (UnsupportedJwtException | IllegalArgumentException e) {
			log.error("액세스 토큰 오류: {}", e.getMessage());
			throw new BusinessException(ErrorCode.ACCESS_TOKEN_MALFORMED, e);
		}
	}

	/**
	 * 리프레시 토큰 유효성 검증 (상세 예외 발생)
	 * 클라이언트가 적절한 에러 처리를 할 수 있도록 구체적인 ErrorCode 제공
	 *
	 * @param token 검증할 리프레시 토큰
	 * @throws BusinessException 토큰이 유효하지 않은 경우
	 */
	public void validateRefreshToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
		} catch (ExpiredJwtException e) {
			log.debug("리프레시 토큰 만료: {}", e.getMessage());
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, e);
		} catch (SecurityException | MalformedJwtException e) {
			log.error("리프레시 토큰 서명 또는 형식 오류: {}", e.getMessage());
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_MALFORMED, e);
		} catch (UnsupportedJwtException | IllegalArgumentException e) {
			log.error("리프레시 토큰 오류: {}", e.getMessage());
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_MALFORMED, e);
		}
	}
}
