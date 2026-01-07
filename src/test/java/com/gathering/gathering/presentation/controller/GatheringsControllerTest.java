package com.gathering.gathering.presentation.controller;

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
import com.gathering.ApiDocSpec;
import com.gathering.auth.application.AuthService;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.gathering.application.GatheringService;
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.CreateGatheringResponse;

/**
 * GatheringsController Spring REST Docs 테스트
 */
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class GatheringsControllerTest {

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
	private ObjectMapper objectMapper;

	@Autowired
	private GatheringService gatheringService;

	@Autowired
	private AuthService authService;

	@BeforeEach
	void setUp() {
		Mockito.reset(gatheringService, authService);
	}

	@Test
	@DisplayName("유효한 정보로 모임을 생성하면 201 상태코드와 생성된 모임 정보를 반환한다")
	void createGatheringSuccess() throws Exception {
		// given: 모임 생성 요청 데이터와 예상 응답 데이터를 준비
		String userTsid = "01HQUSER123456";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("테스트 모임")
			.description("테스트 설명입니다")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.mainImageUrl("https://example.com/image.jpg")
			.build();

		CreateGatheringResponse response = CreateGatheringResponse.builder()
			.tsid("01HQGATHERING1")
			.name(request.getName())
			.description(request.getDescription())
			.regionTsid(request.getRegionTsid())
			.category(request.getCategory())
			.mainImageUrl(request.getMainImageUrl())
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.createGathering(eq(userTsid), any(CreateGatheringRequest.class)))
			.thenReturn(response);

		// when: POST /gatherings 요청을 전송
		// then: 201 상태코드와 생성된 모임 정보를 검증
		mockMvc.perform(post("/gatherings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tsid").value("01HQGATHERING1"))
			.andExpect(jsonPath("$.name").value("테스트 모임"))
			.andExpect(jsonPath("$.description").value("테스트 설명입니다"))
			.andExpect(jsonPath("$.regionTsid").value("01HQREGION1234"))
			.andExpect(jsonPath("$.category").value("SPORTS"))
			.andExpect(jsonPath("$.maxParticipants").value(100))
			.andDo(document("gatherings",
				ApiDocSpec.GATHERING_CREATE.getDescription(),
				ApiDocSpec.GATHERING_CREATE.getSummary(),
				requestFields(
					fieldWithPath("name").description("모임 이름 (필수, 25자 이하)"),
					fieldWithPath("description").description("모임 설명 (선택, 1000자 이하)").optional(),
					fieldWithPath("regionTsid").description("지역 TSID (필수)"),
					fieldWithPath("category").description("모임 카테고리 (필수)"),
					fieldWithPath("mainImageUrl").description("대표 이미지 URL (선택)").optional()
				),
				responseFields(
					fieldWithPath("tsid").description("모임 고유 ID"),
					fieldWithPath("name").description("모임 이름"),
					fieldWithPath("description").description("모임 설명").optional(),
					fieldWithPath("regionTsid").description("지역 TSID"),
					fieldWithPath("category").description("모임 카테고리"),
					fieldWithPath("mainImageUrl").description("대표 이미지 URL").optional(),
					fieldWithPath("maxParticipants").description("최대 참가 인원 (기본값: 100)"),
					fieldWithPath("createdAt").description("생성 일시")
				)
			));
	}

	@Test
	@DisplayName("모임 이름을 누락하고 생성 요청 시 400 에러를 반환한다")
	void createGatheringWithoutName() throws Exception {
		// given: 이름이 없는 모임 생성 요청 데이터를 준비
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.description("테스트 설명")
			.regionTsid("01HQREGION1234")
			.category(GatheringCategory.SPORTS)
			.build();

		// when: POST /gatherings 요청을 전송하고 응답을 검증
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(post("/gatherings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("카테고리를 누락하고 생성 요청 시 400 에러를 반환한다")
	void createGatheringWithoutCategory() throws Exception {
		// given: 카테고리가 없는 모임 생성 요청 데이터를 준비
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("테스트 모임")
			.regionTsid("01HQREGION1234")
			.build();

		// when: POST /gatherings 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(post("/gatherings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("존재하지 않는 지역으로 생성 요청 시 404 에러를 반환한다")
	void createGatheringWithInvalidRegion() throws Exception {
		// given: 존재하지 않는 지역 TSID로 모임 생성 요청 데이터를 준비
		String userTsid = "01HQUSER123456";
		CreateGatheringRequest request = CreateGatheringRequest.builder()
			.name("테스트 모임")
			.regionTsid("INVALID_REGION")
			.category(GatheringCategory.SPORTS)
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.createGathering(eq(userTsid), any(CreateGatheringRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REGION_NOT_FOUND));

		// when: POST /gatherings 요청을 전송
		// then: 404 Not Found 응답과 에러 정보를 확인
		mockMvc.perform(post("/gatherings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.REGION_NOT_FOUND.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.REGION_NOT_FOUND.getMessage()));
	}
}
