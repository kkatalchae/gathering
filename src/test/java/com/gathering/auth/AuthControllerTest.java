package com.gathering.auth;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
import com.gathering.auth.presentation.dto.LoginRequest;
import com.gathering.auth.presentation.dto.LoginResponse;

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
	@DisplayName("POST /login - 로그인 성공")
	void login_success() throws Exception {
		// given
		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password("password1234!")
			.build();

		LoginResponse response = LoginResponse.builder()
			.accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
			.refreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
			.tokenType("Bearer")
			.expiresIn(3600L)
			.build();

		when(authService.login(any(LoginRequest.class))).thenReturn(response);

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists())
			.andExpect(jsonPath("$.refreshToken").exists())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600))
			.andDo(document("auth-login",
				requestFields(
					fieldWithPath("email").description("이메일 주소"),
					fieldWithPath("password").description("비밀번호")
				),
				responseFields(
					fieldWithPath("accessToken").description("액세스 토큰 (JWT)"),
					fieldWithPath("refreshToken").description("리프레시 토큰 (JWT)"),
					fieldWithPath("tokenType").description("토큰 타입 (Bearer)"),
					fieldWithPath("expiresIn").description("액세스 토큰 만료 시간 (초)")
				)
			));

		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	@Test
	@DisplayName("POST /login - 로그인 실패: 존재하지 않는 사용자")
	void login_fail_user_not_found() throws Exception {
		// given
		LoginRequest request = LoginRequest.builder()
			.email("notfound@example.com")
			.password("password1234!")
			.build();

		when(authService.login(any(LoginRequest.class)))
			.thenThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다"));

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	@Test
	@DisplayName("POST /login - 로그인 실패: 잘못된 비밀번호")
	void login_fail_bad_credentials() throws Exception {
		// given
		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password("wrongpassword")
			.build();

		when(authService.login(any(LoginRequest.class)))
			.thenThrow(new BadCredentialsException("비밀번호가 일치하지 않습니다"));

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	@Test
	@DisplayName("POST /login - 로그인 실패: 유효하지 않은 이메일 형식")
	void login_fail_invalid_email() throws Exception {
		// given
		LoginRequest request = LoginRequest.builder()
			.email("invalid-email")
			.password("password1234!")
			.build();

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /login - 로그인 실패: 빈 비밀번호")
	void login_fail_empty_password() throws Exception {
		// given
		LoginRequest request = LoginRequest.builder()
			.email("test@example.com")
			.password("")
			.build();

		// when & then
		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}
}