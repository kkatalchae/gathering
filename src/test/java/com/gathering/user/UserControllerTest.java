package com.gathering.user;

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
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.user.application.UserService;
import com.gathering.user.presentation.dto.UserJoinRequest;

/**
 * UsersController Spring REST Docs 테스트
 */
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public UserService userService() {
			return Mockito.mock(UserService.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserService userService;

	@BeforeEach
	void setUp() {
		Mockito.reset(userService);
	}

	@Test
	@DisplayName("POST /users/join - 회원가입")
	void join() throws Exception {
		// given
		UserJoinRequest request = UserJoinRequest.builder()
			.email("test@example.com")
			.password("password1234!")
			.nickname("테스터")
			.name("홍길동")
			.phoneNumber("01012345678")
			.build();

		doNothing().when(userService).join(any(UserJoinRequest.class));

		// when & then
		mockMvc.perform(post("/users/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent())
			.andDo(document("users-join",
				requestFields(
					fieldWithPath("email").description("이메일 주소 (이메일 형식)"),
					fieldWithPath("password").description("비밀번호"),
					fieldWithPath("nickname").description("닉네임 (선택 사항)").optional(),
					fieldWithPath("name").description("사용자 이름"),
					fieldWithPath("phoneNumber").description("전화번호 (숫자만 입력, 예: 01012345678)")
				)
			));

		verify(userService, times(1)).join(any(UserJoinRequest.class));
	}
}
