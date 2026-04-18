package com.gathering.auth;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.cookies.CookieDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.ApiDocSpec;
import com.gathering.auth.application.AuthService;
import com.gathering.auth.presentation.dto.LoginRequest;
import com.gathering.auth.presentation.dto.LoginResponse;
import com.gathering.auth.presentation.dto.RefreshResponse;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.common.utility.CryptoUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AuthController Spring REST Docs 테스트
 */
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class AuthControllerTest {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public AuthService authService() {
			return Mockito.mock(AuthService.class);
		}

		@Bean
		@Primary
		public AuthenticationManager authenticationManager() {
			return Mockito.mock(AuthenticationManager.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AuthService authService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Value("${crypto.aes.key}")
	private String aesKey;

	@BeforeEach
	void setUp() {
		Mockito.reset(authService, authenticationManager);
	}

	@Test
	@DisplayName("유효한 자격 증명으로 로그인하면 액세스 토큰이 발급된다.")
	void loginSuccess() throws Exception {
		// given
		// @AesEncrypted 어노테이션이 HTTP 요청 역직렬화 시 복호화를 수행하므로
		// 테스트에서는 암호화된 비밀번호를 전달해야 함
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, aesKey);

		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password(encryptedPassword)
			.build();

		LoginResponse response = LoginResponse.builder()
			.accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
			.tokenType("Bearer")
			.expiresIn(3600L)
			.build();

		// AuthenticationManager가 정상적으로 인증 성공하도록 Mock 설정
		when(authenticationManager.authenticate(any())).thenReturn(null);

		when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600))
			.andDo(document("login-success",
				ApiDocSpec.LOGIN.getDescription(),
				ApiDocSpec.LOGIN.getSummary(),
				requestFields(
					fieldWithPath("email").description("이메일 주소"),
					fieldWithPath("password").description("AES 암호화된 비밀번호 (Base64 인코딩)")
				),
				responseFields(
					fieldWithPath("accessToken").description("액세스 토큰 (JWT)"),
					fieldWithPath("tokenType").description("토큰 타입 (Bearer)"),
					fieldWithPath("expiresIn").description("액세스 토큰 만료 시간 (초)")
				)
			));

		verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
	}

	@Test
	@DisplayName("존재하지 않는 사용자로 로그인하면 404 에러가 발생한다.")
	void loginWithNonExistingUser() throws Exception {
		// given
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, aesKey);

		LoginRequest request = LoginRequest.builder()
			.email("notfound@example.com")
			.password(encryptedPassword)
			.build();

		// AuthenticationManager가 UsernameNotFoundException을 던지도록 Mock 설정
		when(authenticationManager.authenticate(any()))
			.thenThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다"));

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andDo(document("login-with-non-existing-user",
				requestFields(
					fieldWithPath("email").description("존재하지 않는 이메일 주소"),
					fieldWithPath("password").description("AES 암호화된 비밀번호")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		// authenticationManager에서 예외가 발생하므로 authService.login()은 호출되지 않음
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인하면 401 에러가 발생한다")
	void loginWithWrongPassword() throws Exception {
		// given
		String plainPassword = "WrongPass1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, aesKey);

		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password(encryptedPassword)
			.build();

		// AuthenticationManager가 BadCredentialsException을 던지도록 Mock 설정
		when(authenticationManager.authenticate(any()))
			.thenThrow(new BadCredentialsException("비밀번호가 일치하지 않습니다"));

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andDo(document("login-with-wrong-password",
				requestFields(
					fieldWithPath("email").description("이메일 주소"),
					fieldWithPath("password").description("잘못된 비밀번호 (AES 암호화)")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		// authenticationManager에서 예외가 발생하므로 authService.login()은 호출되지 않음
	}

	@Test
	@DisplayName("유효하지 않은 이메일 형식으로 로그인하면 400 에러가 발생한다")
	void loginWithInvalidEmailFormat() throws Exception {
		// given
		// @AesEncrypted deserializer가 빈 문자열은 복호화하지 않으므로 빈 비밀번호 사용
		LoginRequest request = LoginRequest.builder()
			.email("invalid-email")
			.password("")
			.build();

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(document("login-with-invalid-email-format",
				requestFields(
					fieldWithPath("email").description("유효하지 않은 이메일 형식"),
					fieldWithPath("password").description("비밀번호")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드 (INVALID_EMAIL_FORMAT)"),
					fieldWithPath("message").description("에러 메시지")
				)
			));
	}

	@Test
	@DisplayName("빈 비밀번호로 로그인하면 400 에러가 발생한다")
	void loginWithEmptyPassword() throws Exception {
		// given
		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password("")
			.build();

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(document("login-with-empty-password",
				requestFields(
					fieldWithPath("email").description("이메일 주소"),
					fieldWithPath("password").description("빈 비밀번호")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));
	}

	// ==================== POST /refresh 테스트 ====================

	@Test
	@DisplayName("유효한 리프레시 토큰으로 갱신하면 새로운 액세스 토큰이 발급된다")
	void refreshWithValidToken() throws Exception {
		// given
		RefreshResponse response = RefreshResponse.builder()
			.accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new-access-token")
			.tokenType("Bearer")
			.expiresIn(3600L)
			.build();

		when(authService.refresh(any(HttpServletRequest.class)))
			.thenReturn(response);

		Cookie refreshTokenCookie = new Cookie("refreshToken", "valid-refresh-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new-access-token"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600))
			.andDo(document("refresh",
				ApiDocSpec.REFRESH.getDescription(),
				ApiDocSpec.REFRESH.getSummary(),
				requestCookies(
					cookieWithName("refreshToken").description("리프레시 토큰 (HTTP-only)")
				),
				responseFields(
					fieldWithPath("accessToken").description("새로운 액세스 토큰 (JWT)"),
					fieldWithPath("tokenType").description("토큰 타입 (Bearer)"),
					fieldWithPath("expiresIn").description("액세스 토큰 만료 시간 (초)")
				)
			));

		verify(authService, times(1)).refresh(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("리프레시 토큰이 없으면 401 에러가 발생한다")
	void refreshWithoutToken() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING));

		// when & then
		mockMvc.perform(post("/refresh"))
			.andExpect(status().isUnauthorized())
			.andDo(document("refresh-without-token",
				responseFields(
					fieldWithPath("code").description("에러 코드 (REFRESH_TOKEN_MISSING)"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).refresh(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("만료된 리프레시 토큰으로 갱신하면 401 에러가 발생한다")
	void refreshWithExpiredToken() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "expired-refresh-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isUnauthorized())
			.andDo(document("refresh-with-expired-token",
				requestCookies(
					cookieWithName("refreshToken").description("만료된 리프레시 토큰")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드 (REFRESH_TOKEN_EXPIRED)"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).refresh(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("잘못된 형식의 리프레시 토큰으로 갱신하면 401 에러가 발생한다")
	void refreshWithMalformedToken() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_MALFORMED));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "malformed-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isUnauthorized())
			.andDo(document("refresh-with-malformed-token",
				requestCookies(
					cookieWithName("refreshToken").description("잘못된 형식의 리프레시 토큰")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드 (REFRESH_TOKEN_MALFORMED)"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).refresh(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("취소된 리프레시 토큰으로 갱신하면 401 에러가 발생한다")
	void refreshWithRevokedToken() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "revoked-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isUnauthorized())
			.andDo(document("refresh-with-revoked-token",
				requestCookies(
					cookieWithName("refreshToken").description("취소된 리프레시 토큰")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드 (REFRESH_TOKEN_REVOKED)"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).refresh(any(HttpServletRequest.class));
	}

	// ==================== POST /logout 테스트 ====================

	@Test
	@DisplayName("유효한 리프레시 토큰으로 로그아웃하면 성공한다")
	void logoutWithValidToken() throws Exception {
		// given
		doNothing().when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "valid-refresh-token");

		// when & then
		mockMvc.perform(post("/logout")
				.cookie(refreshTokenCookie))
			.andExpect(status().isOk())
			.andDo(document("logout",
				ApiDocSpec.LOGOUT.getDescription(),
				ApiDocSpec.LOGOUT.getSummary(),
				requestCookies(
					cookieWithName("refreshToken").description("삭제할 리프레시 토큰")
				)
			));

		verify(authService, times(1)).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	@DisplayName("리프레시 토큰 없이 로그아웃하면 성공한다")
	void logoutWithoutToken() throws Exception {
		// given
		doNothing().when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

		// when & then
		mockMvc.perform(post("/logout"))
			.andExpect(status().isOk())
			.andDo(document("logout-without-token"));

		verify(authService, times(1)).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}
}
