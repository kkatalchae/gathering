package com.gathering.user;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.gathering.auth.application.AuthService;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.application.UserService;

@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PATCH /users/me/profile-image 통합 테스트")
class UserProfileImageUploadControllerTest {

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
	private UserService userService;

	@Autowired
	private AuthService authService;

	private static final String USER_TSID = "testUserTsid00";
	private static final String PROFILE_IMAGE_URL = "/uploads/profile-images/uuid.jpg";

	@BeforeEach
	void setUp() {
		Mockito.reset(userService, authService);
		given(authService.getCurrentUserTsid(any())).willReturn(USER_TSID);
	}

	@Test
	@DisplayName("유효한 JPEG 파일 업로드 - 200 OK, URL 반환")
	void uploadProfileImage_validJpeg_returns200() throws Exception {
		// given
		MockMultipartFile file = createValidJpegFile("photo.jpg");
		given(userService.uploadProfileImage(eq(USER_TSID), any())).willReturn(PROFILE_IMAGE_URL);

		// when & then
		mockMvc.perform(multipart("/users/me/profile-image")
				.file(file)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.url").value(PROFILE_IMAGE_URL))
			.andDo(document("users-upload-profile-image",
				responseFields(
					fieldWithPath("url").description("업로드된 프로필 이미지 URL")
				)
			));
	}

	@Test
	@DisplayName("빈 파일 업로드 - 400 Bad Request, FILE_EMPTY")
	void uploadProfileImage_emptyFile_returns400() throws Exception {
		// given
		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
		given(userService.uploadProfileImage(eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_EMPTY));

		// when & then
		mockMvc.perform(multipart("/users/me/profile-image")
				.file(emptyFile)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FILE_EMPTY"));
	}

	@Test
	@DisplayName("허용되지 않은 확장자 - 400 Bad Request, FILE_EXTENSION_NOT_ALLOWED")
	void uploadProfileImage_invalidExtension_returns400() throws Exception {
		// given
		MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[1024]);
		given(userService.uploadProfileImage(eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED));

		// when & then
		mockMvc.perform(multipart("/users/me/profile-image")
				.file(pdfFile)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FILE_EXTENSION_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("파일 크기 초과 - 400 Bad Request, FILE_SIZE_EXCEEDED")
	void uploadProfileImage_fileSizeExceeded_returns400() throws Exception {
		// given
		MockMultipartFile bigFile = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[1024]);
		given(userService.uploadProfileImage(eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED));

		// when & then
		mockMvc.perform(multipart("/users/me/profile-image")
				.file(bigFile)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FILE_SIZE_EXCEEDED"));
	}

	@Test
	@DisplayName("MIME 타입 위장 파일 - 400 Bad Request, FILE_MIME_TYPE_NOT_ALLOWED")
	void uploadProfileImage_mimeSpoofed_returns400() throws Exception {
		// given
		MockMultipartFile spoofedFile = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
			"<?php echo 'xss'; ?>".getBytes());
		given(userService.uploadProfileImage(eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED));

		// when & then
		mockMvc.perform(multipart("/users/me/profile-image")
				.file(spoofedFile)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FILE_MIME_TYPE_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("존재하지 않는 사용자 - 404 Not Found")
	void uploadProfileImage_userNotFound_returns404() throws Exception {
		// given
		MockMultipartFile file = createValidJpegFile("photo.jpg");
		given(userService.uploadProfileImage(eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

		// when & then
		mockMvc.perform(multipart("/users/me/profile-image")
				.file(file)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	private MockMultipartFile createValidJpegFile(String filename) {
		byte[] content = new byte[1024];
		content[0] = (byte)0xFF;
		content[1] = (byte)0xD8;
		content[2] = (byte)0xFF;
		content[3] = (byte)0xE0;
		return new MockMultipartFile("file", filename, "image/jpeg", content);
	}
}
