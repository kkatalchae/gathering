package com.gathering.auth.infra;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터
 * HTTP 요청의 쿠키에서 JWT 토큰을 추출하여 인증 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserDetailsService userDetailsService;

	private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		try {
			// 1. 쿠키에서 JWT 토큰 추출
			String jwt = extractJwtFromCookie(request);

			// 2. 토큰이 있고 유효한 경우 인증 정보 설정
			if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
				// 3. 토큰에서 사용자 이메일 추출
				String email = jwtTokenProvider.getEmailFromToken(jwt);

				// 4. UserDetailsService를 통해 사용자 정보 로드
				UserDetails userDetails = userDetailsService.loadUserByUsername(email);

				// 5. Authentication 객체 생성
				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
					);

				// 6. 요청 정보 추가 (IP, Session ID 등)
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				// 7. SecurityContext에 인증 정보 설정
				SecurityContextHolder.getContext().setAuthentication(authentication);

				log.debug("JWT 인증 성공: {}", email);
			}
		} catch (Exception e) {
			log.error("JWT 인증 처리 중 오류 발생", e);
			// 인증 실패 시 SecurityContext를 비워서 인증되지 않은 상태로 유지
			SecurityContextHolder.clearContext();
		}

		// 8. 다음 필터로 요청 전달
		filterChain.doFilter(request, response);
	}

	/**
	 * HTTP 요청의 쿠키에서 JWT 토큰 추출
	 *
	 * @param request HttpServletRequest
	 * @return JWT 토큰 (없으면 null)
	 */
	private String extractJwtFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		return Arrays.stream(cookies)
			.filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst()
			.orElse(null);
	}
}