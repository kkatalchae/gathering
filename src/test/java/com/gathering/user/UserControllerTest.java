package com.gathering.user;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;

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
import com.gathering.auth.application.exception.BusinessException;
import com.gathering.auth.application.exception.ErrorCode;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.presentation.dto.UserJoinRequest;
import com.gathering.util.CryptoUtil;

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
		// @AesEncrypted 어노테이션이 HTTP 요청 역직렬화 시 복호화를 수행하므로
		// 테스트에서는 암호화된 비밀번호를 전달해야 함
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, "gatheringkey1234");

		UserJoinRequest request = UserJoinRequest.builder()
			.email("test@example.com")
			.password(encryptedPassword)
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
					fieldWithPath("password").description("AES 암호화된 비밀번호 (Base64 인코딩)"),
					fieldWithPath("nickname").description("닉네임 (선택 사항)").optional(),
					fieldWithPath("name").description("사용자 이름"),
					fieldWithPath("phoneNumber").description("전화번호 (숫자만 입력, 예: 01012345678)")
				)
			));

		verify(userService, times(1)).join(any(UserJoinRequest.class));
	}

	@Test
	@DisplayName("GET /users/{tsid} - 사용자 정보 조회 (정상)")
	void getUserInfo() throws Exception {
		// given
		String tsid = "1234567890123";
		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@example.com")
			.nickname("테스터")
			.name("홍길동")
			.phoneNumber("01012345678")
			.profileImageUrl("https://example.com/profile.jpg")
			.status(UserStatus.ACTIVE)
			.createdAt(Instant.parse("2024-01-01T00:00:00Z"))
			.build();

		when(userService.getUserInfo(tsid)).thenReturn(user);

		// when & then
		mockMvc.perform(get("/users/{tsid}", tsid)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("테스터"))
			.andExpect(jsonPath("$.name").value("홍길동"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://example.com/profile.jpg"))
			.andExpect(jsonPath("$.email").doesNotExist())
			.andExpect(jsonPath("$.phoneNumber").doesNotExist())
			.andExpect(jsonPath("$.tsid").doesNotExist())
			.andDo(document("users-get",
				responseFields(
					fieldWithPath("nickname").description("닉네임"),
					fieldWithPath("name").description("사용자 이름"),
					fieldWithPath("profileImageUrl").description("프로필 이미지 URL").optional()
				)
			));

		verify(userService, times(1)).getUserInfo(tsid);
	}

	@Test
	@DisplayName("GET /users/{tsid} - 삭제된 사용자 조회")
	void getUserInfo_Deleted() throws Exception {
		// given
		String tsid = "1234567890123";

		when(userService.getUserInfo(tsid))
			.thenThrow(new BusinessException(ErrorCode.USER_DELETED));

		// when & then
		mockMvc.perform(get("/users/{tsid}", tsid)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_DELETED"))
			.andExpect(jsonPath("$.message").value("삭제된 사용자입니다."))
			.andDo(document("users-get-deleted",
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(userService, times(1)).getUserInfo(tsid);
	}

	@Test
	@DisplayName("GET /users/{tsid} - 정지된 사용자 조회")
	void getUserInfo_Banned() throws Exception {
		// given
		String tsid = "1234567890123";

		when(userService.getUserInfo(tsid))
			.thenThrow(new BusinessException(ErrorCode.USER_BANNED));

		// when & then
		mockMvc.perform(get("/users/{tsid}", tsid)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_BANNED"))
			.andExpect(jsonPath("$.message").value("사용이 제한된 사용자입니다."))
			.andDo(document("users-get-banned",
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(userService, times(1)).getUserInfo(tsid);
	}
}
