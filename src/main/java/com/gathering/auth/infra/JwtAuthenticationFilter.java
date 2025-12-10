package com.gathering.auth.infra;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터
 * HTTP 요청의 Authorization 헤더에서 JWT 토큰을 추출하여 인증 처리
 * OAuth 2.0 스타일: "Authorization: Bearer {token}"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserDetailsService userDetailsService;
	private final UsersRepository usersRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		try {
			// 1. Authorization 헤더에서 JWT 토큰 추출
			String jwt = extractJwtFromRequest(request);

			// 2. 토큰이 있으면 검증 및 인증 정보 설정
			if (jwt != null) {
				// 3. 액세스 토큰 검증 (BusinessException 발생)
				jwtTokenProvider.validateAccessToken(jwt);

				// 4. 토큰에서 사용자 TSID 추출
				String tsid = jwtTokenProvider.getTsidFromToken(jwt);

				// 5. TSID로 사용자 조회하여 이메일 가져오기
				UsersEntity user = usersRepository.findById(tsid).orElse(null);
				if (user == null) {
					log.debug("사용자를 찾을 수 없습니다: {}", tsid);
					filterChain.doFilter(request, response);
					return;
				}

				String email = user.getEmail();

				// 6. UserDetailsService를 통해 사용자 정보 로드
				UserDetails userDetails = userDetailsService.loadUserByUsername(email);

				// 7. Authentication 객체 생성
				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
					);

				// 8. 요청 정보 추가 (IP, Session ID 등)
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				// 9. SecurityContext에 인증 정보 설정
				SecurityContextHolder.getContext().setAuthentication(authentication);

				log.debug("JWT 인증 성공: {}", email);
			}
		} catch (BusinessException e) {
			// validateAccessToken()에서 발생한 BusinessException 처리
			ErrorCode errorCode = e.getErrorCode();
			log.debug("JWT 인증 실패: {} - {}", errorCode.name(), errorCode.getMessage());

			// 상세한 에러 응답 전송
			sendErrorResponse(
				response,
				errorCode.getHttpStatus(),
				errorCode.name(),  // 예: "ACCESS_TOKEN_EXPIRED"
				errorCode.getMessage()  // 예: "액세스 토큰이 만료되었습니다"
			);
			return; // 필터 체인 중단!

		} catch (Exception e) {
			// 기타 예상치 못한 예외
			log.error("JWT 인증 처리 중 예상치 못한 오류: ", e);
			sendErrorResponse(
				response,
				HttpStatus.UNAUTHORIZED,
				"AUTHENTICATION_FAILED",
				"인증 처리 중 오류가 발생했습니다"
			);
			return;
		}

		// 10. 다음 필터로 요청 전달
		filterChain.doFilter(request, response);
	}

	/**
	 * 에러 응답을 JSON 형식으로 전송
	 */
	private void sendErrorResponse(
		HttpServletResponse response,
		HttpStatus status,
		String code,
		String message
	) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// ErrorResponse 형식과 동일하게 JSON 생성
		String jsonResponse = String.format(
			"{\"code\":\"%s\",\"message\":\"%s\"}",
			code,
			message
		);

		response.getWriter().write(jsonResponse);
		response.getWriter().flush();
	}

	/**
	 * HTTP 요청의 Authorization 헤더에서 JWT 토큰 추출
	 * OAuth 2.0 스타일: "Authorization: Bearer {token}" 형식
	 *
	 * @param request HttpServletRequest
	 * @return JWT 토큰 (없으면 null)
	 */
	private String extractJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (bearerToken != null && bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
			return bearerToken.substring(AuthConstants.BEARER_PREFIX.length());
		}

		return null;
	}
}