package com.gathering.auth.infra;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 실패 시 처리하는 EntryPoint
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출됨
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		log.warn("인증 실패: {}", authException.getMessage());
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다");
	}
}
