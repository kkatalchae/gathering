package com.gathering.auth.infra;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
		return createToken(tsid, accessTokenValidityInSeconds);
	}

	/**
	 * 리프레시 토큰 생성
	 */
	public String createRefreshToken(String tsid) {
		return createToken(tsid, refreshTokenValidityInSeconds);
	}

	/**
	 * JWT 토큰 생성 (공통 로직)
	 */
	private String createToken(String subject, long validityInSeconds) {
		Instant now = Instant.now();
		Instant expiryDate = now.plusSeconds(validityInSeconds);

		return Jwts.builder()
			.subject(subject)
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
	 * 토큰에서 만료 시간 추출
	 */
	public Instant getExpirationFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		return claims.getExpiration().toInstant();
	}

	/**
	 * 토큰에서 모든 Claims 추출
	 */
	public Claims getAllClaimsFromToken(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	/**
	 * 토큰 유효성 검증
	 */
	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.error("잘못된 JWT 서명입니다.", e);
		} catch (ExpiredJwtException e) {
			log.debug("만료된 JWT 토큰입니다. 토큰 갱신이 필요합니다.");
		} catch (UnsupportedJwtException e) {
			log.error("지원되지 않는 JWT 토큰입니다.", e);
		} catch (IllegalArgumentException e) {
			log.error("JWT 토큰이 잘못되었습니다.", e);
		}
		return false;
	}
}