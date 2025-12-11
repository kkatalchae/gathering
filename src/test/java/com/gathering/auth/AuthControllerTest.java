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
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.auth.application.AuthService;
import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.auth.presentation.dto.LoginRequest;
import com.gathering.auth.presentation.dto.LoginResponse;
import com.gathering.auth.presentation.dto.RefreshResponse;
import com.gathering.util.CryptoUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AuthController Spring REST Docs 테스트
 */
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public AuthService authService() {
			return Mockito.mock(AuthService.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AuthService authService;

	@BeforeEach
	void setUp() {
		Mockito.reset(authService);
	}

	@Test
	@DisplayName("유효한_자격증명으로_로그인하면_액세스_토큰이_발급된다")
	void 유효한_자격증명으로_로그인하면_액세스_토큰이_발급된다() throws Exception {
		// given
		// @AesEncrypted 어노테이션이 HTTP 요청 역직렬화 시 복호화를 수행하므로
		// 테스트에서는 암호화된 비밀번호를 전달해야 함
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, "gatheringkey1234");

		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password(encryptedPassword)
			.build();

		LoginResponse response = LoginResponse.builder()
			.accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
			.tokenType("Bearer")
			.expiresIn(3600L)
			.build();

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
			.andDo(document("auth-login",
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
	@DisplayName("존재하지_않는_사용자로_로그인하면_401_에러가_발생한다")
	void 존재하지_않는_사용자로_로그인하면_401_에러가_발생한다() throws Exception {
		// given
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, "gatheringkey1234");

		LoginRequest request = LoginRequest.builder()
			.email("notfound@example.com")
			.password(encryptedPassword)
			.build();

		when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
			.thenThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다"));

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andDo(document("auth-login-fail-user-not-found",
				requestFields(
					fieldWithPath("email").description("존재하지 않는 이메일 주소"),
					fieldWithPath("password").description("AES 암호화된 비밀번호")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
	}

	@Test
	@DisplayName("잘못된_비밀번호로_로그인하면_401_에러가_발생한다")
	void 잘못된_비밀번호로_로그인하면_401_에러가_발생한다() throws Exception {
		// given
		String plainPassword = "WrongPass1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, "gatheringkey1234");

		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password(encryptedPassword)
			.build();

		when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
			.thenThrow(new BadCredentialsException("비밀번호가 일치하지 않습니다"));

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andDo(document("auth-login-fail-bad-credentials",
				requestFields(
					fieldWithPath("email").description("이메일 주소"),
					fieldWithPath("password").description("잘못된 비밀번호 (AES 암호화)")
				),
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).login(any(LoginRequest.class), any(HttpServletResponse.class));
	}

	@Test
	@DisplayName("유효하지_않은_이메일_형식으로_로그인하면_400_에러가_발생한다")
	void 유효하지_않은_이메일_형식으로_로그인하면_400_에러가_발생한다() throws Exception {
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
			.andDo(document("auth-login-fail-invalid-email",
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
	@DisplayName("빈_비밀번호로_로그인하면_400_에러가_발생한다")
	void 빈_비밀번호로_로그인하면_400_에러가_발생한다() throws Exception {
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
			.andDo(document("auth-login-fail-empty-password",
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
	@DisplayName("유효한_리프레시_토큰으로_갱신하면_새로운_액세스_토큰이_발급된다")
	void 유효한_리프레시_토큰으로_갱신하면_새로운_액세스_토큰이_발급된다() throws Exception {
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
			.andDo(document("auth-refresh",
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
	@DisplayName("리프레시_토큰이_없으면_401_에러가_발생한다")
	void 리프레시_토큰이_없으면_401_에러가_발생한다() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING));

		// when & then
		mockMvc.perform(post("/refresh"))
			.andExpect(status().isUnauthorized())
			.andDo(document("auth-refresh-fail-missing-token",
				responseFields(
					fieldWithPath("code").description("에러 코드 (REFRESH_TOKEN_MISSING)"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).refresh(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("만료된_리프레시_토큰으로_갱신하면_401_에러가_발생한다")
	void 만료된_리프레시_토큰으로_갱신하면_401_에러가_발생한다() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "expired-refresh-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isUnauthorized())
			.andDo(document("auth-refresh-fail-expired",
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
	@DisplayName("잘못된_형식의_리프레시_토큰으로_갱신하면_401_에러가_발생한다")
	void 잘못된_형식의_리프레시_토큰으로_갱신하면_401_에러가_발생한다() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_MALFORMED));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "malformed-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isUnauthorized())
			.andDo(document("auth-refresh-fail-malformed",
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
	@DisplayName("취소된_리프레시_토큰으로_갱신하면_401_에러가_발생한다")
	void 취소된_리프레시_토큰으로_갱신하면_401_에러가_발생한다() throws Exception {
		// given
		when(authService.refresh(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "revoked-token");

		// when & then
		mockMvc.perform(post("/refresh")
				.cookie(refreshTokenCookie))
			.andExpect(status().isUnauthorized())
			.andDo(document("auth-refresh-fail-revoked",
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
	@DisplayName("유효한_리프레시_토큰으로_로그아웃하면_성공한다")
	void 유효한_리프레시_토큰으로_로그아웃하면_성공한다() throws Exception {
		// given
		doNothing().when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

		Cookie refreshTokenCookie = new Cookie("refreshToken", "valid-refresh-token");

		// when & then
		mockMvc.perform(post("/logout")
				.cookie(refreshTokenCookie))
			.andExpect(status().isOk())
			.andDo(document("auth-logout",
				requestCookies(
					cookieWithName("refreshToken").description("삭제할 리프레시 토큰")
				)
			));

		verify(authService, times(1)).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}

	@Test
	@DisplayName("리프레시_토큰_없이_로그아웃하면_성공한다")
	void 리프레시_토큰_없이_로그아웃하면_성공한다() throws Exception {
		// given
		doNothing().when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

		// when & then
		mockMvc.perform(post("/logout"))
			.andExpect(status().isOk())
			.andDo(document("auth-logout-without-token"));

		verify(authService, times(1)).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
	}
}
