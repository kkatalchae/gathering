package com.gathering.auth.infra;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.auth.application.AuthService;
import com.gathering.auth.domain.OAuthPrincipal;
import com.gathering.auth.presentation.dto.LoginResponse;
import com.gathering.user.domain.model.UsersEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthSuccessHandler 테스트")
class OAuthSuccessHandlerTest {

	@Mock
	private AuthService authService;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private OAuthSuccessHandler oAuthSuccessHandler;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private Authentication authentication;

	@BeforeEach
	void setUp() {
		// Mock 객체는 @Mock 어노테이션으로 자동 생성됨
	}

	@Test
	@DisplayName("OAuth_로그인_모드_시_JWT_토큰_발급_및_리다이렉트한다")
	void OAuth_로그인_모드_시_JWT_토큰_발급_및_리다이렉트한다() throws Exception {
		// ===== Given: 테스트에 필요한 데이터 준비 및 Mock 동작 정의 =====
		String tsid = "1234567890123";
		String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
		String tokenType = "Bearer";
		long expiresIn = 3600L;

		// OAuthPrincipal 생성 (linkMode = false, 로그인 모드)
		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@gmail.com")
			.name("테스트사용자")
			.build();
		Map<String, Object> attributes = new HashMap<>();
		OAuthPrincipal principal = new OAuthPrincipal(user, attributes, false); // linkMode = false

		// AuthService.login()이 반환할 LoginResponse 객체 생성
		LoginResponse loginResponse = LoginResponse.builder()
			.accessToken(accessToken)
			.tokenType(tokenType)
			.expiresIn(expiresIn)
			.build();

		// Mock 동작 정의:
		when(authentication.getPrincipal()).thenReturn(principal);
		when(authService.login(any(HttpServletResponse.class), eq(tsid))).thenReturn(loginResponse);

		// ===== When: 실제 테스트 대상 메서드 실행 =====
		oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);

		// ===== Then: 예상한 대로 동작했는지 검증 =====

		// 1. authentication.getPrincipal()이 호출되었는지 검증
		verify(authentication, times(1)).getPrincipal();

		// 2. authService.login()이 올바른 파라미터로 1번 호출되었는지 검증
		verify(authService, times(1)).login(eq(response), eq(tsid));

		// 3. response.sendRedirect()가 올바른 URL로 호출되었는지 검증
		String expectedRedirectUrl = String.format("/?accessToken=%s&tokenType=%s&expiresIn=%d",
			URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
			URLEncoder.encode(tokenType, StandardCharsets.UTF_8),
			expiresIn
		);

		verify(response, times(1)).sendRedirect(expectedRedirectUrl);
	}
}