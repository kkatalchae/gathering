package com.gathering.gathering.presentation.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

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
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.GatheringDetailResponse;
import com.gathering.gathering.presentation.dto.GatheringListItemResponse;
import com.gathering.gathering.presentation.dto.GatheringListRequest;
import com.gathering.gathering.presentation.dto.GatheringListResponse;
import com.gathering.gathering.presentation.dto.GatheringResponse;
import com.gathering.gathering.presentation.dto.ParticipantSummary;
import com.gathering.gathering.presentation.dto.UpdateGatheringRequest;

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

		GatheringResponse response = GatheringResponse.builder()
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

	@Test
	@DisplayName("모임 목록을 조회하면 200 상태코드와 모임 목록을 반환한다")
	void getGatheringsSuccess() throws Exception {
		// given: 모임 목록 응답 데이터를 준비
		List<GatheringListItemResponse> items = List.of(
			GatheringListItemResponse.builder()
				.tsid("01HQGATHERING1")
				.name("주말 축구 모임")
				.mainImageUrl("https://example.com/image1.jpg")
				.category(GatheringCategory.SPORTS)
				.regionName("강남구")
				.build(),
			GatheringListItemResponse.builder()
				.tsid("01HQGATHERING2")
				.name("평일 스터디 모임")
				.mainImageUrl("https://example.com/image2.jpg")
				.category(GatheringCategory.STUDY)
				.regionName("서초구")
				.build()
		);

		GatheringListResponse response = GatheringListResponse.builder()
			.gatherings(items)
			.nextCursor("01HQGATHERING3")
			.hasNext(true)
			.build();

		when(gatheringService.getGatherings(any(GatheringListRequest.class)))
			.thenReturn(response);

		// when: GET /gatherings 요청을 전송
		// then: 200 상태코드와 모임 목록을 검증하고 REST Docs 문서화
		mockMvc.perform(get("/gatherings")
				.param("categories", "SPORTS", "STUDY")
				.param("regionTsids", "01HQREGION1")
				.param("cursor", "01HQGATHERING0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.gatherings").isArray())
			.andExpect(jsonPath("$.gatherings[0].tsid").value("01HQGATHERING1"))
			.andExpect(jsonPath("$.gatherings[0].name").value("주말 축구 모임"))
			.andExpect(jsonPath("$.gatherings[0].category").value("SPORTS"))
			.andExpect(jsonPath("$.gatherings[0].regionName").value("강남구"))
			.andExpect(jsonPath("$.nextCursor").value("01HQGATHERING3"))
			.andExpect(jsonPath("$.hasNext").value(true))
			.andDo(document("gatherings-list",
				ApiDocSpec.GATHERING_LIST.getDescription(),
				ApiDocSpec.GATHERING_LIST.getSummary(),
				queryParameters(
					parameterWithName("categories").description("카테고리 필터 (다중 선택 가능)").optional(),
					parameterWithName("regionTsids").description("지역 TSID 필터 (다중 선택 가능)").optional(),
					parameterWithName("cursor").description("페이지 커서 (이전 응답의 nextCursor 값)").optional(),
					parameterWithName("size").description("페이지 크기 (기본: 20, 최대: 100)").optional()
				),
				responseFields(
					fieldWithPath("gatherings[]").description("모임 목록"),
					fieldWithPath("gatherings[].tsid").description("모임 고유 ID"),
					fieldWithPath("gatherings[].name").description("모임 이름"),
					fieldWithPath("gatherings[].mainImageUrl").description("대표 이미지 URL").optional(),
					fieldWithPath("gatherings[].category").description("모임 카테고리"),
					fieldWithPath("gatherings[].regionName").description("지역명"),
					fieldWithPath("nextCursor").description("다음 페이지 커서 (null이면 마지막 페이지)").optional(),
					fieldWithPath("hasNext").description("다음 페이지 존재 여부")
				)
			));
	}

	@Test
	@DisplayName("모임 상세 정보를 조회하면 200 상태코드와 상세 정보를 반환한다")
	void getGatheringDetailSuccess() throws Exception {
		// given: 모임 상세 응답 데이터를 준비
		List<ParticipantSummary> participants = List.of(
			ParticipantSummary.builder()
				.nickname("김철수")
				.profileImageUrl("https://example.com/profile1.jpg")
				.role(ParticipantRole.OWNER)
				.build(),
			ParticipantSummary.builder()
				.nickname("이영희")
				.profileImageUrl("https://example.com/profile2.jpg")
				.role(ParticipantRole.MEMBER)
				.build()
		);

		GatheringDetailResponse response = GatheringDetailResponse.builder()
			.tsid("01HQGATHERING1")
			.name("주말 축구 모임")
			.description("매주 토요일 오전 축구하는 모임입니다.")
			.mainImageUrl("https://example.com/image.jpg")
			.category(GatheringCategory.SPORTS)
			.regionName("강남구")
			.participants(participants)
			.createdAt(Instant.now())
			.build();

		when(gatheringService.getGatheringDetail("01HQGATHERING1"))
			.thenReturn(response);

		// when: GET /gatherings/{tsid} 요청을 전송
		// then: 200 상태코드와 상세 정보를 검증하고 REST Docs 문서화
		mockMvc.perform(get("/gatherings/{tsid}", "01HQGATHERING1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tsid").value("01HQGATHERING1"))
			.andExpect(jsonPath("$.name").value("주말 축구 모임"))
			.andExpect(jsonPath("$.description").value("매주 토요일 오전 축구하는 모임입니다."))
			.andExpect(jsonPath("$.category").value("SPORTS"))
			.andExpect(jsonPath("$.regionName").value("강남구"))
			.andExpect(jsonPath("$.participants").isArray())
			.andExpect(jsonPath("$.participants[0].nickname").value("김철수"))
			.andExpect(jsonPath("$.participants[0].role").value("OWNER"))
			.andDo(document("gatherings-detail",
				ApiDocSpec.GATHERING_DETAIL.getDescription(),
				ApiDocSpec.GATHERING_DETAIL.getSummary(),
				pathParameters(
					parameterWithName("tsid").description("모임 TSID")
				),
				responseFields(
					fieldWithPath("tsid").description("모임 고유 ID"),
					fieldWithPath("name").description("모임 이름"),
					fieldWithPath("description").description("모임 설명").optional(),
					fieldWithPath("mainImageUrl").description("대표 이미지 URL").optional(),
					fieldWithPath("category").description("모임 카테고리"),
					fieldWithPath("regionName").description("지역명"),
					fieldWithPath("participants[]").description("참여자 목록"),
					fieldWithPath("participants[].nickname").description("참여자 닉네임"),
					fieldWithPath("participants[].profileImageUrl").description("참여자 프로필 이미지 URL").optional(),
					fieldWithPath("participants[].role").description("참여자 역할 (OWNER/ADMIN/MEMBER)"),
					fieldWithPath("createdAt").description("생성 일시")
				)
			));
	}

	@Test
	@DisplayName("존재하지 않는 모임 조회 시 404 에러를 반환한다")
	void getGatheringDetailNotFound() throws Exception {
		// given: 존재하지 않는 모임 TSID로 조회 시 예외 발생
		when(gatheringService.getGatheringDetail("INVALID_TSID"))
			.thenThrow(new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

		// when: GET /gatherings/{tsid} 요청을 전송
		// then: 404 Not Found 응답과 에러 정보를 확인
		mockMvc.perform(get("/gatherings/{tsid}", "INVALID_TSID"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_NOT_FOUND.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("OWNER가 유효한 정보로 모임을 수정하면 200 상태코드와 수정된 정보를 반환한다")
	void updateGatheringSuccess() throws Exception {
		// given: 모임 수정 요청 데이터와 예상 응답 데이터를 준비
		String userTsid = "01HQUSER123456";
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.description("수정된 설명입니다")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.mainImageUrl("https://example.com/new-image.jpg")
			.build();

		GatheringResponse response = GatheringResponse.builder()
			.tsid(gatheringTsid)
			.name(request.getName())
			.description(request.getDescription())
			.regionTsid(request.getRegionTsid())
			.category(request.getCategory())
			.mainImageUrl(request.getMainImageUrl())
			.maxParticipants(100)
			.createdAt(Instant.now())
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.updateGathering(eq(gatheringTsid), eq(userTsid), any(UpdateGatheringRequest.class)))
			.thenReturn(response);

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 200 상태코드와 수정된 모임 정보를 검증
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tsid").value(gatheringTsid))
			.andExpect(jsonPath("$.name").value("수정된 모임"))
			.andExpect(jsonPath("$.description").value("수정된 설명입니다"))
			.andExpect(jsonPath("$.regionTsid").value("01HQREGION5678"))
			.andExpect(jsonPath("$.category").value("STUDY"))
			.andExpect(jsonPath("$.mainImageUrl").value("https://example.com/new-image.jpg"))
			.andDo(document("gatherings-update",
				ApiDocSpec.GATHERING_UPDATE.getDescription(),
				ApiDocSpec.GATHERING_UPDATE.getSummary(),
				pathParameters(
					parameterWithName("tsid").description("모임 TSID")
				),
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
					fieldWithPath("maxParticipants").description("최대 참가 인원"),
					fieldWithPath("createdAt").description("생성 일시")
				)
			));
	}

	@Test
	@DisplayName("모임 이름을 누락하고 수정 요청 시 400 에러를 반환한다")
	void updateGatheringWithoutName() throws Exception {
		// given: 이름이 없는 모임 수정 요청 데이터를 준비
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.description("수정된 설명")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("카테고리를 누락하고 수정 요청 시 400 에러를 반환한다")
	void updateGatheringWithoutCategory() throws Exception {
		// given: 카테고리가 없는 모임 수정 요청 데이터를 준비
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.build();

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("지역을 누락하고 수정 요청 시 400 에러를 반환한다")
	void updateGatheringWithoutRegion() throws Exception {
		// given: 지역이 없는 모임 수정 요청 데이터를 준비
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.category(GatheringCategory.STUDY)
			.build();

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("존재하지 않는 모임 수정 시 404 에러를 반환한다")
	void updateGatheringNotFound() throws Exception {
		// given: 존재하지 않는 모임 TSID로 수정 요청
		String userTsid = "01HQUSER123456";
		String invalidTsid = "INVALID_TSID";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.updateGathering(eq(invalidTsid), eq(userTsid), any(UpdateGatheringRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 404 Not Found 응답과 에러 정보를 확인
		mockMvc.perform(put("/gatherings/{tsid}", invalidTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_NOT_FOUND.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("존재하지 않는 지역으로 수정 시 404 에러를 반환한다")
	void updateGatheringWithInvalidRegion() throws Exception {
		// given: 존재하지 않는 지역 TSID로 수정 요청
		String userTsid = "01HQUSER123456";
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("INVALID_REGION")
			.category(GatheringCategory.STUDY)
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.updateGathering(eq(gatheringTsid), eq(userTsid), any(UpdateGatheringRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.REGION_NOT_FOUND));

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 404 Not Found 응답과 에러 정보를 확인
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.REGION_NOT_FOUND.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.REGION_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("MEMBER 권한으로 수정 시 403 에러를 반환한다")
	void updateGatheringWithMemberRole() throws Exception {
		// given: MEMBER 권한으로 수정 요청
		String userTsid = "01HQUSER123456";
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.updateGathering(eq(gatheringTsid), eq(userTsid), any(UpdateGatheringRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.GATHERING_PERMISSION_DENIED));

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 403 Forbidden 응답과 에러 정보를 확인
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_PERMISSION_DENIED.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_PERMISSION_DENIED.getMessage()));
	}

	@Test
	@DisplayName("모임에 참여하지 않은 사용자가 수정 시 403 에러를 반환한다")
	void updateGatheringByNonParticipant() throws Exception {
		// given: 비참여자가 수정 요청
		String userTsid = "01HQNONPARTICIP";
		String gatheringTsid = "01HQGATHERING1";
		UpdateGatheringRequest request = UpdateGatheringRequest.builder()
			.name("수정된 모임")
			.regionTsid("01HQREGION5678")
			.category(GatheringCategory.STUDY)
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		when(gatheringService.updateGathering(eq(gatheringTsid), eq(userTsid), any(UpdateGatheringRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.GATHERING_PERMISSION_DENIED));

		// when: PUT /gatherings/{tsid} 요청을 전송
		// then: 403 Forbidden 응답과 에러 정보를 확인
		mockMvc.perform(put("/gatherings/{tsid}", gatheringTsid)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_PERMISSION_DENIED.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_PERMISSION_DENIED.getMessage()));
	}

	@Test
	@DisplayName("OWNER가 모임을 삭제하면 204 상태코드를 반환한다")
	void deleteGatheringSuccess() throws Exception {
		// given: OWNER의 삭제 요청 준비
		String userTsid = "01HQOWNER12345";
		String gatheringTsid = "01HQGATHERING1";

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		doNothing().when(gatheringService).deleteGathering(gatheringTsid, userTsid);

		// when: DELETE /gatherings/{tsid} 요청을 전송
		// then: 204 상태코드를 검증하고 REST Docs 문서화
		mockMvc.perform(delete("/gatherings/{tsid}", gatheringTsid))
			.andExpect(status().isNoContent())
			.andDo(document("gatherings-delete",
				ApiDocSpec.GATHERING_DELETE.getDescription(),
				ApiDocSpec.GATHERING_DELETE.getSummary(),
				pathParameters(
					parameterWithName("tsid").description("모임 TSID")
				)
			));
	}

	@Test
	@DisplayName("존재하지 않는 모임 삭제 시 404 에러를 반환한다")
	void deleteGatheringNotFound() throws Exception {
		// given: 존재하지 않는 모임 TSID로 삭제 요청
		String userTsid = "01HQOWNER12345";
		String invalidTsid = "INVALID_TSID";

		when(authService.getCurrentUserTsid(any())).thenReturn(userTsid);
		doThrow(new BusinessException(ErrorCode.GATHERING_NOT_FOUND))
			.when(gatheringService).deleteGathering(invalidTsid, userTsid);

		// when: DELETE /gatherings/{tsid} 요청을 전송
		// then: 404 Not Found 응답과 에러 정보를 확인
		mockMvc.perform(delete("/gatherings/{tsid}", invalidTsid))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_NOT_FOUND.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("ADMIN 권한으로 삭제 시 403 에러를 반환한다")
	void deleteGatheringWithAdminRole() throws Exception {
		// given: ADMIN 권한으로 삭제 요청
		String adminTsid = "01HQADMIN12345";
		String gatheringTsid = "01HQGATHERING1";

		when(authService.getCurrentUserTsid(any())).thenReturn(adminTsid);
		doThrow(new BusinessException(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED))
			.when(gatheringService).deleteGathering(gatheringTsid, adminTsid);

		// when: DELETE /gatherings/{tsid} 요청을 전송
		// then: 403 Forbidden 응답과 에러 정보를 확인
		mockMvc.perform(delete("/gatherings/{tsid}", gatheringTsid))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED.getMessage()));
	}

	@Test
	@DisplayName("MEMBER 권한으로 삭제 시 403 에러를 반환한다")
	void deleteGatheringWithMemberRole() throws Exception {
		// given: MEMBER 권한으로 삭제 요청
		String memberTsid = "01HQMEMBER1234";
		String gatheringTsid = "01HQGATHERING1";

		when(authService.getCurrentUserTsid(any())).thenReturn(memberTsid);
		doThrow(new BusinessException(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED))
			.when(gatheringService).deleteGathering(gatheringTsid, memberTsid);

		// when: DELETE /gatherings/{tsid} 요청을 전송
		// then: 403 Forbidden 응답과 에러 정보를 확인
		mockMvc.perform(delete("/gatherings/{tsid}", gatheringTsid))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED.getMessage()));
	}

	@Test
	@DisplayName("모임에 참여하지 않은 사용자가 삭제 시 403 에러를 반환한다")
	void deleteGatheringByNonParticipant() throws Exception {
		// given: 비참여자가 삭제 요청
		String nonParticipantTsid = "01HQNONPARTICIP";
		String gatheringTsid = "01HQGATHERING1";

		when(authService.getCurrentUserTsid(any())).thenReturn(nonParticipantTsid);
		doThrow(new BusinessException(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED))
			.when(gatheringService).deleteGathering(gatheringTsid, nonParticipantTsid);

		// when: DELETE /gatherings/{tsid} 요청을 전송
		// then: 403 Forbidden 응답과 에러 정보를 확인
		mockMvc.perform(delete("/gatherings/{tsid}", gatheringTsid))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED.name()))
			.andExpect(jsonPath("$.message").value(ErrorCode.GATHERING_DELETE_PERMISSION_DENIED.getMessage()));
	}
}
