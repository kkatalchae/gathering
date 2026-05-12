package com.gathering.auth.infra;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인가 실패 시 처리하는 Handler
 * 인증은 되었지만 권한이 부족한 경우 호출됨
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException {
		log.warn("권한 부족: {}", accessDeniedException.getMessage());
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다");
	}
}