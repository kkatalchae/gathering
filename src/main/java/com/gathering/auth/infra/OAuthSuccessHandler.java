package com.gathering.auth.infra;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.presentation.dto.LoginResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {

		String tsid = authentication.getName();
		LoginResponse loginResponse = authService.login(response, tsid);

		// 홈페이지로 리다이렉트하면서 쿼리 파라미터로 토큰 전달
		String redirectUrl = String.format("/?accessToken=%s&tokenType=%s&expiresIn=%d",
			URLEncoder.encode(loginResponse.getAccessToken(), StandardCharsets.UTF_8),
			URLEncoder.encode(loginResponse.getTokenType(), StandardCharsets.UTF_8),
			loginResponse.getExpiresIn()
		);

		response.sendRedirect(redirectUrl);
	}
}
