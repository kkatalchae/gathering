package com.gathering.auth.application;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.auth.application.dto.AuthTokens;
import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.auth.application.exception.InvalidTokenException;
import com.gathering.auth.application.exception.TokenMismatchException;
import com.gathering.auth.infra.AuthConstants;
import com.gathering.auth.infra.CookieUtil;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.auth.presentation.dto.LoginRequest;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final UsersRepository usersRepository;

	/**
	 * 로그인 처리
	 * Spring Security의 AuthenticationManager를 사용하여 인증 수행
	 * 인증 성공 시 JWT 토큰 발급
	 *
	 * Note: 비밀번호는 @AesEncrypted 어노테이션에 의해 DTO 바인딩 시점에 자동으로 복호화됨
	 */
	@Transactional(readOnly = true)
	public AuthTokens login(LoginRequest request) {
		// 1. 인증 시도 (비밀번호는 이미 복호화되어 있음)
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				request.getEmail(),
				request.getPassword()
			)
		);

		// 2. 인증 성공 시 사용자 정보 추출
		String email = authentication.getName();

		// 3. 이메일로 사용자 조회하여 TSID 가져오기
		UsersEntity user = usersRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		String tsid = user.getTsid();

		// 4. JWT 토큰 생성 (TSID를 subject로 사용)
		String accessToken = jwtTokenProvider.createAccessToken(tsid);
		String refreshToken = jwtTokenProvider.createRefreshToken(tsid);

		// 5. RefreshToken을 Redis에 저장
		refreshTokenService.saveRefreshToken(tsid, refreshToken);

		// 6. 토큰 반환
		return AuthTokens.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	/**
	 * 토큰 갱신 처리 (Refresh Token Rotation)
	 * RefreshToken을 검증하고 새로운 AccessToken과 RefreshToken 발급
	 *
	 * @param refreshToken 갱신 요청 토큰
	 * @return 새로운 토큰 쌍
	 * @throws InvalidTokenException RefreshToken이 유효하지 않은 경우
	 * @throws TokenMismatchException RefreshToken이 저장된 값과 일치하지 않는 경우
	 */
	public AuthTokens refresh(String refreshToken) {
		// 1. RefreshToken JWT 검증
		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new InvalidTokenException("RefreshToken이 유효하지 않습니다");
		}

		// 2. 토큰에서 TSID 추출
		String tsid = jwtTokenProvider.getTsidFromToken(refreshToken);

		// 3. Redis에 저장된 RefreshToken과 비교
		if (!refreshTokenService.validateRefreshToken(tsid, refreshToken)) {
			// 검증 실패 시 Redis에서 RefreshToken 삭제 (강제 로그아웃)
			refreshTokenService.deleteRefreshToken(tsid);
			log.warn("RefreshToken 불일치 또는 만료. 강제 로그아웃 처리: {}", tsid);
			throw new TokenMismatchException("RefreshToken이 일치하지 않거나 만료되었습니다");
		}

		// 4. 새로운 토큰 생성 (Rotation)
		String newAccessToken = jwtTokenProvider.createAccessToken(tsid);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(tsid);

		// 5. 새로운 RefreshToken을 Redis에 저장 (기존 토큰 교체)
		refreshTokenService.saveRefreshToken(tsid, newRefreshToken);

		log.debug("토큰 갱신 완료: {}", tsid);

		// 6. 새로운 토큰 반환
		return AuthTokens.builder()
			.accessToken(newAccessToken)
			.refreshToken(newRefreshToken)
			.build();
	}

	/**
	 * 현재 로그인한 사용자의 TSID 추출
	 * 쿠키에서 Access Token을 읽어 JWT를 파싱하여 TSID 반환
	 *
	 * @param request HttpServletRequest
	 * @return 사용자 TSID
	 * @throws InvalidTokenException 토큰이 없거나 유효하지 않은 경우
	 */
	public String getCurrentUserTsid(HttpServletRequest request) {
		// 쿠키에서 Access Token 추출
		String accessToken = CookieUtil.getCookie(request, AuthConstants.ACCESS_TOKEN_COOKIE)
			.orElseThrow(() -> new InvalidTokenException("로그인이 필요합니다"));

		// JWT에서 TSID 추출
		return jwtTokenProvider.getTsidFromToken(accessToken);
	}
}