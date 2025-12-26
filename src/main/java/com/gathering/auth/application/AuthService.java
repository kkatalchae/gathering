package com.gathering.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.auth.infra.AuthConstants;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.auth.presentation.dto.LoginRequest;
import com.gathering.auth.presentation.dto.LoginResponse;
import com.gathering.auth.presentation.dto.RefreshResponse;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.common.utility.CookieUtil;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final UsersRepository usersRepository;

	@Value("${jwt.access-token-validity-in-seconds}")
	private long accessTokenValidityInSeconds;

	@Value("${jwt.refresh-token-validity-in-seconds}")
	private long refreshTokenValidityInSeconds;
	private static final String TOKEN_TYPE = "Bearer";

	/**
	 * 로그인 처리 (OAuth 2.0 스타일)
	 * Spring Security의 AuthenticationManager를 사용하여 인증 수행
	 * - AccessToken: 응답 본문에 포함
	 * - RefreshToken: HTTP-only 쿠키로 설정
	 * Note: 비밀번호는 @AesEncrypted 어노테이션에 의해 DTO 바인딩 시점에 자동으로 복호화됨
	 */
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request, HttpServletResponse response) {

		// 2. 인증 성공 시 사용자 정보 추출
		String email = request.getEmail();

		// 3. 이메일로 사용자 조회하여 TSID 가져오기
		UsersEntity user = usersRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		String tsid = user.getTsid();

		return login(response, tsid);
	}

	/**
	 * 토큰 갱신 처리 (OAuth 2.0 스타일)
	 * RefreshToken을 검증하고 새로운 AccessToken 발급
	 * - AccessToken: 응답 본문에 포함
	 * - RefreshToken: 변경 없음 (90일 유효기간)
	 *
	 * @param request HttpServletRequest (쿠키에서 RefreshToken 추출)
	 * @return 새로운 AccessToken 정보
	 * @throws BusinessException RefreshToken이 유효하지 않은 경우
	 */
	public RefreshResponse refresh(HttpServletRequest request) {
		// 1. 쿠키에서 RefreshToken 추출
		String refreshToken = CookieUtil.getCookie(request, AuthConstants.REFRESH_TOKEN_COOKIE)
			.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING));

		// 2. RefreshToken JWT 검증 (상세 예외 발생)
		jwtTokenProvider.validateRefreshToken(refreshToken);

		// 3. 토큰에서 TSID와 JTI 추출
		String tsid = jwtTokenProvider.getTsidFromToken(refreshToken);
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		// 4. Redis에 저장된 RefreshToken과 비교 (멀티 디바이스 지원)
		if (!refreshTokenService.validateRefreshToken(tsid, jti, refreshToken)) {
			// Redis에 토큰이 없으면 로그아웃되었거나 TTL이 만료된 것
			log.warn("RefreshToken이 Redis에 없음 (로그아웃 또는 TTL 만료): tsid={}, jti={}", tsid, jti);
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED);
		}

		// 5. 새로운 AccessToken 생성 (RefreshToken은 재사용)
		String newAccessToken = jwtTokenProvider.createAccessToken(tsid);

		log.info("토큰 갱신 완료: tsid={}, jti={}", tsid, jti);

		// 6. 새로운 AccessToken은 응답 본문으로 반환
		return RefreshResponse.builder()
			.accessToken(newAccessToken)
			.tokenType(TOKEN_TYPE)
			.expiresIn(accessTokenValidityInSeconds)
			.build();
	}

	/**
	 * 로그아웃 처리
	 * Redis에서 RefreshToken을 삭제하여 토큰 갱신 불가능하도록 만듦
	 * 특정 기기만 로그아웃 (멀티 디바이스 지원)
	 *
	 * @param request HttpServletRequest (쿠키에서 RefreshToken 추출)
	 * @param response HttpServletResponse (쿠키 삭제)
	 */
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		// 1. 쿠키에서 RefreshToken 추출
		String refreshToken = CookieUtil.getCookie(request, AuthConstants.REFRESH_TOKEN_COOKIE)
			.orElse(null);

		if (refreshToken == null) {
			return;
		}

		try {
			// 2. RefreshToken JWT 검증 (상세 예외 발생)
			jwtTokenProvider.validateRefreshToken(refreshToken);
		} catch (BusinessException ex) {
			log.warn("유효하지 않은 RefreshToken으로 로그아웃 시도: {}", ex.getMessage());
			CookieUtil.deleteCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE);
			return;
		}

		// 2. 토큰에서 TSID와 JTI 추출
		String tsid = jwtTokenProvider.getTsidFromToken(refreshToken);
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		// 3. Redis에서 해당 RefreshToken 삭제 (특정 기기만 로그아웃)
		refreshTokenService.deleteRefreshToken(tsid, jti);
		log.info("로그아웃 완료: tsid={}, jti={}", tsid, jti);

		// 4. RefreshToken 쿠키 삭제
		CookieUtil.deleteCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE);
	}

	public LoginResponse login(HttpServletResponse response, String tsid) {

		// 2. JWT 토큰 생성
		String accessToken = jwtTokenProvider.createAccessToken(tsid);
		String refreshToken = jwtTokenProvider.createRefreshToken(tsid);

		// 3. RefreshToken에서 JTI 추출
		String jti = jwtTokenProvider.getJtiFromToken(refreshToken);

		// 4. RefreshToken을 Redis에 저장
		refreshTokenService.saveRefreshToken(tsid, jti, refreshToken);

		// 5. RefreshToken을 HTTP-only 쿠키로 설정
		CookieUtil.addSecureCookie(
			response,
			AuthConstants.REFRESH_TOKEN_COOKIE,
			refreshToken,
			(int)refreshTokenValidityInSeconds
		);

		// 6. AccessToken 응답
		return LoginResponse.builder()
			.accessToken(accessToken)
			.tokenType(TOKEN_TYPE)
			.expiresIn(accessTokenValidityInSeconds)
			.build();
	}

	/**
	 * 현재 로그인한 사용자의 TSID 추출
	 * Authorization 헤더에서 Access Token을 읽어 JWT를 파싱하여 TSID 반환
	 * OAuth 2.0 스타일: "Authorization: Bearer {token}"
	 *
	 * @param request HttpServletRequest
	 * @return 사용자 TSID
	 * @throws BusinessException 토큰이 없거나 유효하지 않은 경우
	 */
	public String getCurrentUserTsid(HttpServletRequest request) {
		// Authorization 헤더에서 Bearer 토큰 추출
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (bearerToken == null || !bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
			throw new BusinessException(ErrorCode.ACCESS_TOKEN_MISSING);
		}

		String accessToken = bearerToken.substring(AuthConstants.BEARER_PREFIX.length());

		// JWT에서 TSID 추출
		return jwtTokenProvider.getTsidFromToken(accessToken);
	}

	/**
	 * 현재 로그인한 사용자의 TSID 추출 (Optional)
	 * 비로그인 상태인 경우 null 반환
	 *
	 * @param request HttpServletRequest
	 * @return 사용자 TSID (비로그인 시 null)
	 */
	public String getCurrentUserTsidOrNull(HttpServletRequest request) {
		try {
			return getCurrentUserTsid(request);
		} catch (BusinessException e) {
			return null;
		}
	}

	/**
	 * RefreshToken 쿠키에서 JTI 추출
	 * OAuth 인증 과정에서 session 일관성 검증을 위해 사용
	 *
	 * @param request HttpServletRequest
	 * @return RefreshToken의 JTI (쿠키가 없거나 유효하지 않으면 null)
	 */
	public String getRefreshTokenJtiOrNull(HttpServletRequest request) {
		return CookieUtil.getCookie(request, AuthConstants.REFRESH_TOKEN_COOKIE)
			.map(refreshToken -> {
				try {
					jwtTokenProvider.validateRefreshToken(refreshToken);
					return jwtTokenProvider.getJtiFromToken(refreshToken);
				} catch (BusinessException e) {
					log.debug("유효하지 않은 RefreshToken: {}", e.getMessage());
					return null;
				}
			})
			.orElse(null);
	}
}
