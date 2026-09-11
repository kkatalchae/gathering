package com.gathering.schedule.presentation.controller;

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
import com.gathering.schedule.application.ScheduleService;
import com.gathering.schedule.presentation.dto.CreateScheduleRequest;
import com.gathering.schedule.presentation.dto.ScheduleDetailResponse;
import com.gathering.schedule.presentation.dto.ScheduleListItemResponse;
import com.gathering.schedule.presentation.dto.ScheduleListRequest;
import com.gathering.schedule.presentation.dto.ScheduleListResponse;
import com.gathering.schedule.presentation.dto.ScheduleResponse;
import com.gathering.schedule.presentation.dto.UpdateScheduleRequest;

/**
 * SchedulesController Spring REST Docs 테스트
 */
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class SchedulesControllerTest {

	private static final String USER_TSID = "01HQUSERHOST01";
	private static final String SCHEDULE_TSID = "01HQSCHEDULE01";
	private static final String GATHERING_TSID = "01HQGATHERING1";
	private static final Instant START_AT = Instant.parse("2026-09-20T10:00:00Z");

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public ScheduleService scheduleService() {
			return Mockito.mock(ScheduleService.class);
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
	private ScheduleService scheduleService;

	@Autowired
	private AuthService authService;

	@BeforeEach
	void setUp() {
		Mockito.reset(scheduleService, authService);
	}

	@Test
	@DisplayName("유효한 정보로 일정을 생성하면 201 상태코드와 생성된 일정 정보를 반환한다")
	void createScheduleSuccess() throws Exception {
		// given: 모임에 귀속된 일정 생성 요청과 예상 응답을 준비
		CreateScheduleRequest request = CreateScheduleRequest.builder()
			.gatheringTsid(GATHERING_TSID)
			.title("9월 정기 모임")
			.description("한강에서 러닝 후 저녁 식사")
			.startAt(START_AT)
			.endAt(START_AT.plusSeconds(7200))
			.locationName("한강공원 2주차장")
			.locationAddress("서울특별시 영등포구 여의동로 330")
			.maxParticipants(4)
			.build();

		ScheduleResponse response = ScheduleResponse.builder()
			.tsid(SCHEDULE_TSID)
			.gatheringTsid(GATHERING_TSID)
			.title(request.getTitle())
			.description(request.getDescription())
			.startAt(request.getStartAt())
			.endAt(request.getEndAt())
			.locationName(request.getLocationName())
			.locationAddress(request.getLocationAddress())
			.maxParticipants(request.getMaxParticipants())
			.hostTsid(USER_TSID)
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);
		when(scheduleService.createSchedule(eq(USER_TSID), any(CreateScheduleRequest.class))).thenReturn(response);

		// when: POST /schedules 요청을 전송
		// then: 201 상태코드와 생성된 일정 정보를 검증
		mockMvc.perform(post("/schedules")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tsid").value(SCHEDULE_TSID))
			.andExpect(jsonPath("$.gatheringTsid").value(GATHERING_TSID))
			.andExpect(jsonPath("$.title").value("9월 정기 모임"))
			.andExpect(jsonPath("$.locationName").value("한강공원 2주차장"))
			.andExpect(jsonPath("$.maxParticipants").value(4))
			.andExpect(jsonPath("$.hostTsid").value(USER_TSID))
			.andDo(document("schedules",
				ApiDocSpec.SCHEDULE_CREATE.getDescription(),
				ApiDocSpec.SCHEDULE_CREATE.getSummary(),
				requestFields(
					fieldWithPath("gatheringTsid").description("귀속할 모임 TSID (선택, 없으면 독립 일정)").optional(),
					fieldWithPath("title").description("일정 제목 (필수, 50자 이하)"),
					fieldWithPath("description").description("일정 설명 (선택, 1000자 이하)").optional(),
					fieldWithPath("startAt").description("시작 시각 (필수, ISO-8601)"),
					fieldWithPath("endAt").description("종료 시각 (선택, 시작 시각 이후)").optional(),
					fieldWithPath("locationName").description("장소명 (필수, 100자 이하)"),
					fieldWithPath("locationAddress").description("장소 주소 (선택, 255자 이하)").optional(),
					fieldWithPath("maxParticipants").description("정원 (선택, 없으면 인원 제한 없음)").optional()
				),
				responseFields(
					fieldWithPath("tsid").description("일정 고유 ID"),
					fieldWithPath("gatheringTsid").description("귀속 모임 TSID (독립 일정이면 null)").optional(),
					fieldWithPath("title").description("일정 제목"),
					fieldWithPath("description").description("일정 설명").optional(),
					fieldWithPath("startAt").description("시작 시각"),
					fieldWithPath("endAt").description("종료 시각").optional(),
					fieldWithPath("locationName").description("장소명"),
					fieldWithPath("locationAddress").description("장소 주소").optional(),
					fieldWithPath("maxParticipants").description("정원 (null이면 인원 제한 없음)").optional(),
					fieldWithPath("hostTsid").description("호스트(개설자) TSID"),
					fieldWithPath("createdAt").description("생성 일시"),
					fieldWithPath("updatedAt").description("최종 수정 일시").optional()
				)
			));
	}

	@Test
	@DisplayName("장소명을 누락하고 일정 생성 요청 시 400 에러를 반환한다")
	void createScheduleWithoutLocationName() throws Exception {
		// given: 장소명이 없는 일정 생성 요청을 준비
		CreateScheduleRequest request = CreateScheduleRequest.builder()
			.title("9월 정기 모임")
			.startAt(START_AT)
			.build();

		// when: POST /schedules 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(post("/schedules")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("시작 시각을 누락하고 일정 생성 요청 시 400 에러를 반환한다")
	void createScheduleWithoutStartAt() throws Exception {
		// given: 시작 시각이 없는 일정 생성 요청을 준비
		CreateScheduleRequest request = CreateScheduleRequest.builder()
			.title("9월 정기 모임")
			.locationName("한강공원 2주차장")
			.build();

		// when: POST /schedules 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(post("/schedules")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("모임에 참여하지 않은 사용자가 그 모임의 일정을 만들면 403 에러를 반환한다")
	void createScheduleWithoutGatheringParticipation() throws Exception {
		// given: 모임 참여자가 아닌 사용자의 일정 생성 요청을 준비
		CreateScheduleRequest request = CreateScheduleRequest.builder()
			.gatheringTsid(GATHERING_TSID)
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);
		when(scheduleService.createSchedule(eq(USER_TSID), any(CreateScheduleRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.NOT_GATHERING_PARTICIPANT));

		// when: POST /schedules 요청을 전송
		// then: 403 Forbidden 응답을 확인
		mockMvc.perform(post("/schedules")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("일정 목록 조회 시 200 상태코드와 일정 목록, 페이지네이션 정보를 반환한다")
	void getSchedulesSuccess() throws Exception {
		// given: 일정 목록 응답을 준비
		ScheduleListItemResponse item = ScheduleListItemResponse.builder()
			.tsid(SCHEDULE_TSID)
			.gatheringTsid(GATHERING_TSID)
			.gatheringName("한강 러닝크루")
			.title("9월 정기 모임")
			.startAt(START_AT)
			.endAt(START_AT.plusSeconds(7200))
			.locationName("한강공원 2주차장")
			.maxParticipants(4)
			.participantCount(2L)
			.hostTsid(USER_TSID)
			.hostNickname("러닝맨")
			.build();
		ScheduleListResponse response = ScheduleListResponse.of(
			List.of(item), START_AT + "_" + SCHEDULE_TSID, true);

		when(scheduleService.getSchedules(any(ScheduleListRequest.class))).thenReturn(response);

		// when: GET /schedules 요청을 전송
		// then: 200 상태코드와 목록 정보를 검증
		mockMvc.perform(get("/schedules")
				.param("gatheringTsid", GATHERING_TSID)
				.param("timeFilter", "UPCOMING")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.schedules[0].tsid").value(SCHEDULE_TSID))
			.andExpect(jsonPath("$.schedules[0].participantCount").value(2))
			.andExpect(jsonPath("$.hasNext").value(true))
			.andDo(document("schedules-list",
				ApiDocSpec.SCHEDULE_LIST.getDescription(),
				ApiDocSpec.SCHEDULE_LIST.getSummary(),
				queryParameters(
					parameterWithName("gatheringTsid").description("모임 TSID 필터 (선택, 없으면 전체 일정)").optional(),
					parameterWithName("timeFilter").description("UPCOMING(기본) 또는 PAST").optional(),
					parameterWithName("cursor").description("페이지 커서 (선택, 첫 페이지는 생략)").optional(),
					parameterWithName("size").description("페이지 크기 (기본: 20, 최대: 100)").optional()
				),
				responseFields(
					fieldWithPath("schedules").description("일정 목록"),
					fieldWithPath("schedules[].tsid").description("일정 고유 ID"),
					fieldWithPath("schedules[].gatheringTsid").description("귀속 모임 TSID (독립 일정이면 null)").optional(),
					fieldWithPath("schedules[].gatheringName").description("귀속 모임 이름 (독립 일정이면 null)").optional(),
					fieldWithPath("schedules[].title").description("일정 제목"),
					fieldWithPath("schedules[].startAt").description("시작 시각"),
					fieldWithPath("schedules[].endAt").description("종료 시각").optional(),
					fieldWithPath("schedules[].locationName").description("장소명"),
					fieldWithPath("schedules[].maxParticipants").description("정원 (null이면 인원 제한 없음)").optional(),
					fieldWithPath("schedules[].participantCount").description("현재 참여 인원"),
					fieldWithPath("schedules[].hostTsid").description("호스트 TSID"),
					fieldWithPath("schedules[].hostNickname").description("호스트 닉네임").optional(),
					fieldWithPath("nextCursor").description("다음 페이지 커서 (마지막 페이지면 null)").optional(),
					fieldWithPath("hasNext").description("다음 페이지 존재 여부")
				)
			));
	}

	@Test
	@DisplayName("페이지 크기가 범위를 벗어나면 400 에러를 반환한다")
	void getSchedulesWithInvalidSize() throws Exception {
		// when: size=0 으로 GET /schedules 요청을 전송
		// then: 400 Bad Request 응답을 확인
		mockMvc.perform(get("/schedules").param("size", "0"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일정 상세 조회 시 호스트 정보와 참여 인원을 포함해 반환한다")
	void getScheduleDetailSuccess() throws Exception {
		// given: 일정 상세 응답을 준비
		ScheduleDetailResponse response = ScheduleDetailResponse.builder()
			.tsid(SCHEDULE_TSID)
			.gatheringTsid(GATHERING_TSID)
			.gatheringName("한강 러닝크루")
			.title("9월 정기 모임")
			.description("한강에서 러닝 후 저녁 식사")
			.startAt(START_AT)
			.endAt(START_AT.plusSeconds(7200))
			.locationName("한강공원 2주차장")
			.locationAddress("서울특별시 영등포구 여의동로 330")
			.maxParticipants(4)
			.participantCount(2L)
			.hostTsid(USER_TSID)
			.hostNickname("러닝맨")
			.hostName("김병채")
			.hostProfileImageUrl("https://example.com/host.jpg")
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		when(scheduleService.getScheduleDetail(SCHEDULE_TSID)).thenReturn(response);

		// when: GET /schedules/{scheduleTsid} 요청을 전송
		// then: 200 상태코드와 상세 정보를 검증
		mockMvc.perform(get("/schedules/{scheduleTsid}", SCHEDULE_TSID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tsid").value(SCHEDULE_TSID))
			.andExpect(jsonPath("$.gatheringName").value("한강 러닝크루"))
			.andExpect(jsonPath("$.participantCount").value(2))
			.andExpect(jsonPath("$.hostNickname").value("러닝맨"))
			.andDo(document("schedules-detail",
				ApiDocSpec.SCHEDULE_DETAIL.getDescription(),
				ApiDocSpec.SCHEDULE_DETAIL.getSummary(),
				pathParameters(
					parameterWithName("scheduleTsid").description("일정 TSID")
				),
				responseFields(
					fieldWithPath("tsid").description("일정 고유 ID"),
					fieldWithPath("gatheringTsid").description("귀속 모임 TSID (독립 일정이면 null)").optional(),
					fieldWithPath("gatheringName").description("귀속 모임 이름 (독립 일정이면 null)").optional(),
					fieldWithPath("title").description("일정 제목"),
					fieldWithPath("description").description("일정 설명").optional(),
					fieldWithPath("startAt").description("시작 시각"),
					fieldWithPath("endAt").description("종료 시각").optional(),
					fieldWithPath("locationName").description("장소명"),
					fieldWithPath("locationAddress").description("장소 주소").optional(),
					fieldWithPath("maxParticipants").description("정원 (null이면 인원 제한 없음)").optional(),
					fieldWithPath("participantCount").description("현재 참여 인원"),
					fieldWithPath("hostTsid").description("호스트 TSID"),
					fieldWithPath("hostNickname").description("호스트 닉네임").optional(),
					fieldWithPath("hostName").description("호스트 이름"),
					fieldWithPath("hostProfileImageUrl").description("호스트 프로필 이미지 URL").optional(),
					fieldWithPath("createdAt").description("생성 일시"),
					fieldWithPath("updatedAt").description("최종 수정 일시").optional()
				)
			));
	}

	@Test
	@DisplayName("존재하지 않는 일정을 조회하면 404 에러를 반환한다")
	void getScheduleDetailNotFound() throws Exception {
		// given: 존재하지 않는 일정 조회를 준비
		when(scheduleService.getScheduleDetail(SCHEDULE_TSID))
			.thenThrow(new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		// when: GET /schedules/{scheduleTsid} 요청을 전송
		// then: 404 Not Found 응답을 확인
		mockMvc.perform(get("/schedules/{scheduleTsid}", SCHEDULE_TSID))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("일정을 수정하면 200 상태코드와 수정된 일정 정보를 반환한다")
	void updateScheduleSuccess() throws Exception {
		// given: 일정 수정 요청과 예상 응답을 준비
		UpdateScheduleRequest request = UpdateScheduleRequest.builder()
			.title("9월 정기 모임 (장소 변경)")
			.description("비가 와서 실내로 변경합니다")
			.startAt(START_AT)
			.endAt(START_AT.plusSeconds(7200))
			.locationName("합정역 3번 출구")
			.locationAddress("서울특별시 마포구 양화로 45")
			.maxParticipants(6)
			.build();

		ScheduleResponse response = ScheduleResponse.builder()
			.tsid(SCHEDULE_TSID)
			.gatheringTsid(GATHERING_TSID)
			.title(request.getTitle())
			.description(request.getDescription())
			.startAt(request.getStartAt())
			.endAt(request.getEndAt())
			.locationName(request.getLocationName())
			.locationAddress(request.getLocationAddress())
			.maxParticipants(request.getMaxParticipants())
			.hostTsid(USER_TSID)
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);
		when(scheduleService.updateSchedule(eq(SCHEDULE_TSID), eq(USER_TSID), any(UpdateScheduleRequest.class)))
			.thenReturn(response);

		// when: PUT /schedules/{scheduleTsid} 요청을 전송
		// then: 200 상태코드와 수정된 정보를 검증
		mockMvc.perform(put("/schedules/{scheduleTsid}", SCHEDULE_TSID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("9월 정기 모임 (장소 변경)"))
			.andExpect(jsonPath("$.locationName").value("합정역 3번 출구"))
			.andDo(document("schedules-update",
				ApiDocSpec.SCHEDULE_UPDATE.getDescription(),
				ApiDocSpec.SCHEDULE_UPDATE.getSummary(),
				pathParameters(
					parameterWithName("scheduleTsid").description("일정 TSID")
				),
				requestFields(
					fieldWithPath("title").description("일정 제목 (필수, 50자 이하)"),
					fieldWithPath("description").description("일정 설명 (선택, 1000자 이하)").optional(),
					fieldWithPath("startAt").description("시작 시각 (필수, ISO-8601)"),
					fieldWithPath("endAt").description("종료 시각 (선택, 시작 시각 이후)").optional(),
					fieldWithPath("locationName").description("장소명 (필수, 100자 이하)"),
					fieldWithPath("locationAddress").description("장소 주소 (선택, 255자 이하)").optional(),
					fieldWithPath("maxParticipants").description("정원 (선택, 현재 참여 인원보다 작을 수 없음)").optional()
				),
				responseFields(
					fieldWithPath("tsid").description("일정 고유 ID"),
					fieldWithPath("gatheringTsid").description("귀속 모임 TSID (독립 일정이면 null)").optional(),
					fieldWithPath("title").description("일정 제목"),
					fieldWithPath("description").description("일정 설명").optional(),
					fieldWithPath("startAt").description("시작 시각"),
					fieldWithPath("endAt").description("종료 시각").optional(),
					fieldWithPath("locationName").description("장소명"),
					fieldWithPath("locationAddress").description("장소 주소").optional(),
					fieldWithPath("maxParticipants").description("정원 (null이면 인원 제한 없음)").optional(),
					fieldWithPath("hostTsid").description("호스트 TSID"),
					fieldWithPath("createdAt").description("생성 일시"),
					fieldWithPath("updatedAt").description("최종 수정 일시").optional()
				)
			));
	}

	@Test
	@DisplayName("호스트가 아닌 사용자가 일정을 수정하면 403 에러를 반환한다")
	void updateScheduleWithoutPermission() throws Exception {
		// given: 권한 없는 사용자의 수정 요청을 준비
		UpdateScheduleRequest request = UpdateScheduleRequest.builder()
			.title("몰래 바꾼 제목")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.build();

		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);
		when(scheduleService.updateSchedule(eq(SCHEDULE_TSID), eq(USER_TSID), any(UpdateScheduleRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.SCHEDULE_PERMISSION_DENIED));

		// when: PUT /schedules/{scheduleTsid} 요청을 전송
		// then: 403 Forbidden 응답을 확인
		mockMvc.perform(put("/schedules/{scheduleTsid}", SCHEDULE_TSID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("일정을 삭제하면 204 상태코드를 반환한다")
	void deleteScheduleSuccess() throws Exception {
		// given: 삭제 요청자를 준비
		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);

		// when: DELETE /schedules/{scheduleTsid} 요청을 전송
		// then: 204 No Content 응답을 검증
		mockMvc.perform(delete("/schedules/{scheduleTsid}", SCHEDULE_TSID))
			.andExpect(status().isNoContent())
			.andDo(document("schedules-delete",
				ApiDocSpec.SCHEDULE_DELETE.getDescription(),
				ApiDocSpec.SCHEDULE_DELETE.getSummary(),
				pathParameters(
					parameterWithName("scheduleTsid").description("일정 TSID")
				)
			));

		verify(scheduleService).deleteSchedule(SCHEDULE_TSID, USER_TSID);
	}

	@Test
	@DisplayName("존재하지 않는 일정을 삭제하면 404 에러를 반환한다")
	void deleteScheduleNotFound() throws Exception {
		// given: 존재하지 않는 일정 삭제를 준비
		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);
		doThrow(new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND))
			.when(scheduleService).deleteSchedule(SCHEDULE_TSID, USER_TSID);

		// when: DELETE /schedules/{scheduleTsid} 요청을 전송
		// then: 404 Not Found 응답을 확인
		mockMvc.perform(delete("/schedules/{scheduleTsid}", SCHEDULE_TSID))
			.andExpect(status().isNotFound());
	}
}
