package com.gathering.auth.infra;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.gathering.common.exception.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException, ServletException {

		String errorMessage = "소셜 로그인에 실패했습니다.";

		// BusinessException에서 에러 메시지 추출
		if (exception.getCause() instanceof BusinessException) {
			BusinessException be = (BusinessException)exception.getCause();
			errorMessage = be.getErrorCode().getMessage();
		}

		String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
		response.sendRedirect("/login?error=oauth_failed&message=" + encodedMessage);
	}
}