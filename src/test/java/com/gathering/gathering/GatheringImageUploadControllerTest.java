package com.gathering.gathering;

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
import com.gathering.gathering.application.GatheringService;

@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PATCH /gatherings/{tsid}/main-image 통합 테스트")
class GatheringImageUploadControllerTest {

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		public GatheringService gatheringService() {
			return Mockito.mock(GatheringService.class);
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
	private GatheringService gatheringService;

	@Autowired
	private AuthService authService;

	private static final String GATHERING_TSID = "gatheringTsid0";
	private static final String USER_TSID = "testUserTsid00";
	private static final String IMAGE_URL = "/uploads/gathering-images/uuid.jpg";

	@BeforeEach
	void setUp() {
		Mockito.reset(gatheringService, authService);
		given(authService.getCurrentUserTsid(any())).willReturn(USER_TSID);
	}

	@Test
	@DisplayName("모임 OWNER가 유효한 이미지 업로드 - 200 OK")
	void uploadMainImage_ownerUploads_returns200() throws Exception {
		// given
		MockMultipartFile file = createValidJpegFile("banner.jpg");
		given(gatheringService.uploadMainImage(eq(GATHERING_TSID), eq(USER_TSID), any())).willReturn(IMAGE_URL);

		// when & then
		mockMvc.perform(multipart("/gatherings/{tsid}/main-image", GATHERING_TSID)
				.file(file)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.url").value(IMAGE_URL))
			.andDo(document("gatherings-upload-main-image",
				pathParameters(
					parameterWithName("tsid").description("모임 TSID")
				),
				responseFields(
					fieldWithPath("url").description("업로드된 모임 대표 이미지 URL")
				)
			));
	}

	@Test
	@DisplayName("OWNER 권한 없는 사용자 - 403 Forbidden")
	void uploadMainImage_notOwner_returns403() throws Exception {
		// given
		MockMultipartFile file = createValidJpegFile("banner.jpg");
		given(gatheringService.uploadMainImage(eq(GATHERING_TSID), eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.GATHERING_ACCESS_DENIED));

		// when & then
		mockMvc.perform(multipart("/gatherings/{tsid}/main-image", GATHERING_TSID)
				.file(file)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("GATHERING_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("존재하지 않는 모임 - 404 Not Found")
	void uploadMainImage_gatheringNotFound_returns404() throws Exception {
		// given
		MockMultipartFile file = createValidJpegFile("banner.jpg");
		given(gatheringService.uploadMainImage(eq(GATHERING_TSID), eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

		// when & then
		mockMvc.perform(multipart("/gatherings/{tsid}/main-image", GATHERING_TSID)
				.file(file)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("GATHERING_NOT_FOUND"));
	}

	@Test
	@DisplayName("10MB 초과 파일 - 400 Bad Request, FILE_SIZE_EXCEEDED")
	void uploadMainImage_fileSizeExceeded_returns400() throws Exception {
		// given
		MockMultipartFile bigFile = createValidJpegFile("big.jpg");
		given(gatheringService.uploadMainImage(eq(GATHERING_TSID), eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED));

		// when & then
		mockMvc.perform(multipart("/gatherings/{tsid}/main-image", GATHERING_TSID)
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
	@DisplayName("허용되지 않은 확장자 - 400 Bad Request, FILE_EXTENSION_NOT_ALLOWED")
	void uploadMainImage_invalidExtension_returns400() throws Exception {
		// given
		MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[1024]);
		given(gatheringService.uploadMainImage(eq(GATHERING_TSID), eq(USER_TSID), any()))
			.willThrow(new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED));

		// when & then
		mockMvc.perform(multipart("/gatherings/{tsid}/main-image", GATHERING_TSID)
				.file(pdfFile)
				.with(req -> {
					req.setMethod("PATCH");
					return req;
				})
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FILE_EXTENSION_NOT_ALLOWED"));
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
