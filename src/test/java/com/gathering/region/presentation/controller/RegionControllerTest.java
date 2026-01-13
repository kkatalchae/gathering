package com.gathering.region.presentation.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.web.servlet.MockMvc;

import com.gathering.ApiDocSpec;
import com.gathering.region.application.RegionService;
import com.gathering.region.presentation.dto.RegionResponse;

@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class RegionControllerTest {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public RegionService regionService() {
			return Mockito.mock(RegionService.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegionService regionService;

	@BeforeEach
	void setUp() {
		Mockito.reset(regionService);
	}

	@Test
	@DisplayName("전체 지역 목록을 계층 구조로 조회하면 200 상태코드와 지역 목록을 반환한다")
	void getAllRegionsSuccess() throws Exception {
		// given: 계층 구조 지역 목록 준비
		List<RegionResponse> seoulChildren = List.of(
			RegionResponse.builder()
				.tsid("0R1G1N0000010")
				.code("11680")
				.name("강남구")
				.depth(2)
				.children(List.of())
				.build(),
			RegionResponse.builder()
				.tsid("0R1G1N0000011")
				.code("11740")
				.name("강동구")
				.depth(2)
				.children(List.of())
				.build()
		);

		List<RegionResponse> gyeonggiChildren = List.of(
			RegionResponse.builder()
				.tsid("0R1G1N0000020")
				.code("41131")
				.name("수원시")
				.depth(2)
				.children(List.of())
				.build()
		);

		List<RegionResponse> response = List.of(
			RegionResponse.builder()
				.tsid("0R1G1N0000001")
				.code("11")
				.name("서울특별시")
				.depth(1)
				.children(seoulChildren)
				.build(),
			RegionResponse.builder()
				.tsid("0R1G1N0000002")
				.code("41")
				.name("경기도")
				.depth(1)
				.children(gyeonggiChildren)
				.build()
		);

		when(regionService.getAllRegionsHierarchical()).thenReturn(response);

		// when: GET /regions 요청을 전송
		// then: 200 상태코드와 계층 구조 지역 목록을 검증
		mockMvc.perform(get("/regions"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].tsid").value("0R1G1N0000001"))
			.andExpect(jsonPath("$[0].code").value("11"))
			.andExpect(jsonPath("$[0].name").value("서울특별시"))
			.andExpect(jsonPath("$[0].depth").value(1))
			.andExpect(jsonPath("$[0].children").isArray())
			.andExpect(jsonPath("$[0].children[0].tsid").value("0R1G1N0000010"))
			.andExpect(jsonPath("$[0].children[0].name").value("강남구"))
			.andExpect(jsonPath("$[0].children[0].depth").value(2))
			.andExpect(jsonPath("$[1].name").value("경기도"))
			.andExpect(jsonPath("$[1].children[0].name").value("수원시"))
			.andDo(document("regions-list",
				ApiDocSpec.REGION_LIST.getDescription(),
				ApiDocSpec.REGION_LIST.getSummary(),
				responseFields(
					fieldWithPath("[].tsid").description("지역 고유 ID (모임 필터링에 사용)"),
					fieldWithPath("[].code").description("행정구역 코드"),
					fieldWithPath("[].name").description("지역명"),
					fieldWithPath("[].depth").description("지역 깊이 (1: 시/도, 2: 구/시)"),
					fieldWithPath("[].children").description("하위 지역 목록 (depth-2)"),
					fieldWithPath("[].children[].tsid").description("하위 지역 고유 ID"),
					fieldWithPath("[].children[].code").description("하위 지역 코드"),
					fieldWithPath("[].children[].name").description("하위 지역명"),
					fieldWithPath("[].children[].depth").description("하위 지역 깊이 (2)"),
					fieldWithPath("[].children[].children").description("하위 지역의 children (항상 빈 배열)")
				)
			));
	}
}