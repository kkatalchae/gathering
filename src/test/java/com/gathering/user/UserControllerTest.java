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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.auth.application.AuthService;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.common.utility.CryptoUtil;
import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.presentation.dto.ChangePasswordRequest;
import com.gathering.user.presentation.dto.MyInfoResponse;
import com.gathering.user.presentation.dto.SetPasswordRequest;
import com.gathering.user.presentation.dto.UpdateMyInfoRequest;
import com.gathering.user.presentation.dto.UserJoinRequest;
import com.gathering.user.presentation.dto.WithdrawRequest;

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

		@Bean
		@Primary
		public AuthService authService() {
			return Mockito.mock(AuthService.class);
		}

		@Bean
		@Primary
		public JwtTokenProvider jwtTokenProvider() {
			return Mockito.mock(JwtTokenProvider.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserService userService;

	@Autowired
	private AuthService authService;

	@Value("${crypto.aes.key}")
	private String aesKey;

	@BeforeEach
	void setUp() {
		Mockito.reset(userService, authService);
	}

	@Test
	@DisplayName("POST /users/join - 회원가입")
	void join() throws Exception {
		// given
		// @AesEncrypted 어노테이션이 HTTP 요청 역직렬화 시 복호화를 수행하므로
		// 테스트에서는 암호화된 비밀번호를 전달해야 함
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, aesKey);

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

		when(userService.getUsersEntityByTsid(tsid)).thenReturn(user);

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

		verify(userService, times(1)).getUsersEntityByTsid(tsid);
	}

	@Test
	@DisplayName("GET /users/{tsid} - 삭제된 사용자 조회")
	void getUserInfo_Deleted() throws Exception {
		// given
		String tsid = "1234567890123";

		when(userService.getUsersEntityByTsid(tsid))
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

		verify(userService, times(1)).getUsersEntityByTsid(tsid);
	}

	@Test
	@DisplayName("GET /users/me - 내 정보 조회 (정상)")
	void getMyInfo() throws Exception {
		// given
		String tsid = "1234567890123";

		MyInfoResponse myInfoResponse = MyInfoResponse.builder()
			.tsid(tsid)
			.email("test@example.com")
			.nickname("테스터")
			.name("홍길동")
			.phoneNumber("01012345678")
			.profileImageUrl("https://example.com/profile.jpg")
			.status(UserStatus.ACTIVE)
			.createdAt(Instant.parse("2024-01-01T00:00:00Z"))
			.hasPassword(true)
			.connectedProviders(java.util.List.of())
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		when(userService.getMyInfo(tsid)).thenReturn(myInfoResponse);

		// when & then
		mockMvc.perform(get("/users/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tsid").value(tsid))
			.andExpect(jsonPath("$.email").value("test@example.com"))
			.andExpect(jsonPath("$.nickname").value("테스터"))
			.andExpect(jsonPath("$.name").value("홍길동"))
			.andExpect(jsonPath("$.phoneNumber").value("01012345678"))
			.andExpect(jsonPath("$.profileImageUrl").value("https://example.com/profile.jpg"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.hasPassword").value(true))
			.andExpect(jsonPath("$.connectedProviders").isArray())
			.andDo(document("users-me",
				responseFields(
					fieldWithPath("tsid").description("사용자 고유 ID"),
					fieldWithPath("email").description("이메일"),
					fieldWithPath("nickname").description("닉네임").optional(),
					fieldWithPath("name").description("사용자 이름"),
					fieldWithPath("phoneNumber").description("전화번호").optional(),
					fieldWithPath("profileImageUrl").description("프로필 이미지 URL").optional(),
					fieldWithPath("status").description("계정 상태"),
					fieldWithPath("createdAt").description("가입일시"),
					fieldWithPath("hasPassword").description("비밀번호 설정 여부"),
					fieldWithPath("connectedProviders").description("연동된 소셜 계정 목록 (GOOGLE 등)").optional()
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).getMyInfo(tsid);
	}

	@Test
	@DisplayName("PATCH /users/me - 내 정보 수정 (성공)")
	void updateMyInfo_Success() throws Exception {
		// given
		String tsid = "1234567890123";

		UpdateMyInfoRequest updateRequest = new UpdateMyInfoRequest("새닉네임", "새이름", "01087654321");

		MyInfoResponse updatedResponse = MyInfoResponse.builder()
			.tsid(tsid)
			.email("test@example.com")
			.nickname("새닉네임")
			.name("새이름")
			.phoneNumber("01087654321")
			.profileImageUrl("https://example.com/profile.jpg")
			.status(UserStatus.ACTIVE)
			.createdAt(Instant.parse("2024-01-01T00:00:00Z"))
			.hasPassword(true)
			.connectedProviders(java.util.List.of())
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		when(userService.updateMyInfo(eq(tsid), any(UpdateMyInfoRequest.class))).thenReturn(updatedResponse);

		// when & then
		mockMvc.perform(patch("/users/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("새닉네임"))
			.andExpect(jsonPath("$.name").value("새이름"))
			.andExpect(jsonPath("$.phoneNumber").value("01087654321"))
			.andExpect(jsonPath("$.hasPassword").value(true))
			.andExpect(jsonPath("$.connectedProviders").isArray())
			.andDo(document("users-update-me",
				requestFields(
					fieldWithPath("nickname").description("닉네임 (null이면 변경하지 않음)").optional(),
					fieldWithPath("name").description("이름 (null이면 변경하지 않음)").optional(),
					fieldWithPath("phoneNumber").description("전화번호 (null이면 변경하지 않음, 10-11자리 숫자)").optional()
				),
				responseFields(
					fieldWithPath("tsid").description("사용자 고유 ID"),
					fieldWithPath("email").description("이메일 (변경 불가)"),
					fieldWithPath("nickname").description("닉네임").optional(),
					fieldWithPath("name").description("사용자 이름"),
					fieldWithPath("phoneNumber").description("전화번호").optional(),
					fieldWithPath("profileImageUrl").description("프로필 이미지 URL").optional(),
					fieldWithPath("status").description("계정 상태"),
					fieldWithPath("createdAt").description("가입일시"),
					fieldWithPath("hasPassword").description("비밀번호 설정 여부"),
					fieldWithPath("connectedProviders").description("연동된 소셜 계정 목록 (GOOGLE 등)").optional()
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).updateMyInfo(eq(tsid), any(UpdateMyInfoRequest.class));
	}

	@Test
	@DisplayName("PUT /users/me/password - 비밀번호 변경 (성공)")
	void changePassword_Success() throws Exception {
		// given
		String tsid = "1234567890123";

		String currentPassword = "OldPass1!";
		String newPassword = "NewPass1!";
		String encryptedCurrent = CryptoUtil.encryptAES(currentPassword, aesKey);
		String encryptedNew = CryptoUtil.encryptAES(newPassword, aesKey);

		ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(encryptedCurrent, encryptedNew);

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		doNothing().when(userService).changePassword(eq(tsid), any(ChangePasswordRequest.class));

		// when & then
		mockMvc.perform(put("/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(changePasswordRequest)))
			.andExpect(status().isNoContent())
			.andDo(document("users-change-password",
				requestFields(
					fieldWithPath("currentPassword").description("현재 비밀번호 (AES 암호화)"),
					fieldWithPath("newPassword").description("새 비밀번호 (AES 암호화, 최소 8자, 숫자+특수문자 포함)")
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).changePassword(eq(tsid), any(ChangePasswordRequest.class));
	}

	@Test
	@DisplayName("PUT /users/me/password - 현재 비밀번호 불일치")
	void changePassword_InvalidCurrentPassword() throws Exception {
		// given
		String tsid = "1234567890123";

		String currentPassword = "WrongPass1!";
		String newPassword = "NewPass1!";
		String encryptedCurrent = CryptoUtil.encryptAES(currentPassword, aesKey);
		String encryptedNew = CryptoUtil.encryptAES(newPassword, aesKey);

		ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(encryptedCurrent, encryptedNew);

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		doThrow(new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD))
			.when(userService).changePassword(eq(tsid), any());

		// when & then
		mockMvc.perform(put("/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(changePasswordRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
			.andExpect(jsonPath("$.message").value("현재 비밀번호가 올바르지 않습니다."))
			.andDo(document("users-change-password-invalid-current",
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).changePassword(eq(tsid), any());
	}

	@Test
	@DisplayName("PUT /users/me/password - 새 비밀번호 형식 오류")
	void changePassword_InvalidPasswordFormat() throws Exception {
		// given
		String tsid = "1234567890123";

		String currentPassword = "OldPass1!";
		String newPassword = "weak"; // 비밀번호 정책 위반
		String encryptedCurrent = CryptoUtil.encryptAES(currentPassword, aesKey);
		String encryptedNew = CryptoUtil.encryptAES(newPassword, aesKey);

		ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(encryptedCurrent, encryptedNew);

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		doThrow(new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT))
			.when(userService).changePassword(eq(tsid), any());

		// when & then
		mockMvc.perform(put("/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(changePasswordRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_FORMAT"))
			.andDo(document("users-change-password-invalid-format",
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).changePassword(eq(tsid), any());
	}

	@Test
	@DisplayName("유효한 비밀번호로 비밀번호를 설정하면 204 응답을 반환한다")
	void setPasswordSuccess() throws Exception {
		// given
		String tsid = "1234567890123";
		String newPassword = "NewPassword1!";
		String encryptedPassword = CryptoUtil.encryptAES(newPassword, aesKey);

		SetPasswordRequest request = new SetPasswordRequest(encryptedPassword);

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		doNothing().when(userService).setPassword(eq(tsid), any(SetPasswordRequest.class));

		// when & then
		mockMvc.perform(post("/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent())
			.andDo(document("POST /users/me/password - 비밀번호 설정",
				requestFields(
					fieldWithPath("password").description("설정할 비밀번호 (AES 암호화, 최소 8자, 숫자+특수문자 포함)")
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).setPassword(eq(tsid), any(SetPasswordRequest.class));
	}

	@Test
	@DisplayName("형식이 잘못된 비밀번호로 비밀번호를 설정하면 400 에러를 반환한다")
	void setPasswordWithInvalidFormat() throws Exception {
		// given
		String tsid = "1234567890123";
		String weakPassword = "weak"; // 8자 미만, 숫자/특수문자 없음
		String encryptedPassword = CryptoUtil.encryptAES(weakPassword, aesKey);

		SetPasswordRequest request = new SetPasswordRequest(encryptedPassword);

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		doThrow(new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT))
			.when(userService).setPassword(eq(tsid), any(SetPasswordRequest.class));

		// when & then
		mockMvc.perform(post("/users/me/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_PASSWORD_FORMAT"))
			.andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상이며, 숫자와 특수문자(!@#$%^&*)를 포함해야 합니다."))
			.andDo(document("POST /users/me/password - 비밀번호 형식 검증 실패",
				responseFields(
					fieldWithPath("code").description("에러 코드"),
					fieldWithPath("message").description("에러 메시지")
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).setPassword(eq(tsid), any(SetPasswordRequest.class));
	}

	@Test
	@DisplayName("정상적으로 회원 탈퇴하면 204 응답을 반환한다")
	void withdrawSuccess() throws Exception {
		// given
		String tsid = "1234567890123";
		String plainPassword = "Password1!";
		String encryptedPassword = CryptoUtil.encryptAES(plainPassword, aesKey);

		WithdrawRequest request = new WithdrawRequest(encryptedPassword);

		when(authService.getCurrentUserTsid(any())).thenReturn(tsid);
		doNothing().when(userService).withdraw(eq(tsid), any(WithdrawRequest.class));

		// when & then
		mockMvc.perform(delete("/users/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent())
			.andDo(document("DELETE /users/me - 회원 탈퇴",
				requestFields(
					fieldWithPath("password").description("비밀번호 (AES 암호화, 본인 확인용)")
				)
			));

		verify(authService, times(1)).getCurrentUserTsid(any());
		verify(userService, times(1)).withdraw(eq(tsid), any(WithdrawRequest.class));
	}
}
