package com.gathering.schedule.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.schedule.application.ScheduleService;
import com.gathering.schedule.domain.model.ScheduleTimeFilter;
import com.gathering.schedule.presentation.dto.CreateScheduleRequest;
import com.gathering.schedule.presentation.dto.ScheduleDetailResponse;
import com.gathering.schedule.presentation.dto.ScheduleListRequest;
import com.gathering.schedule.presentation.dto.ScheduleListResponse;
import com.gathering.schedule.presentation.dto.ScheduleResponse;
import com.gathering.schedule.presentation.dto.UpdateScheduleRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 일정 API Controller
 */
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class SchedulesController {

	private final ScheduleService scheduleService;
	private final AuthService authService;

	/**
	 * 일정 생성
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param createRequest 일정 생성 요청 DTO
	 * @return 생성된 일정 정보 (201 Created)
	 */
	@PostMapping
	public ResponseEntity<ScheduleResponse> createSchedule(
		HttpServletRequest request,
		@Valid @RequestBody CreateScheduleRequest createRequest) {

		String hostTsid = authService.getCurrentUserTsid(request);
		ScheduleResponse response = scheduleService.createSchedule(hostTsid, createRequest);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 일정 목록 조회
	 *
	 * @param gatheringTsid 모임 TSID 필터 (선택, 없으면 전체 일정)
	 * @param timeFilter UPCOMING(기본) 또는 PAST
	 * @param cursor 페이지 커서
	 * @param size 페이지 크기 (기본: 20, 최대: 100)
	 * @return 일정 목록 및 페이지네이션 정보
	 */
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ScheduleListResponse> getSchedules(
		@RequestParam(required = false) String gatheringTsid,
		@RequestParam(defaultValue = "UPCOMING") ScheduleTimeFilter timeFilter,
		@RequestParam(required = false) String cursor,
		@RequestParam(defaultValue = "20") int size) {

		ScheduleListRequest request = ScheduleListRequest.builder()
			.gatheringTsid(gatheringTsid)
			.timeFilter(timeFilter)
			.cursor(cursor)
			.size(size)
			.build();

		request.validate();

		ScheduleListResponse response = scheduleService.getSchedules(request);

		return ResponseEntity.ok(response);
	}

	/**
	 * 일정 상세 조회
	 *
	 * @param scheduleTsid 일정 TSID
	 * @return 일정 상세 정보
	 */
	@GetMapping(value = "/{scheduleTsid}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ScheduleDetailResponse> getScheduleDetail(@PathVariable String scheduleTsid) {
		ScheduleDetailResponse response = scheduleService.getScheduleDetail(scheduleTsid);

		return ResponseEntity.ok(response);
	}

	/**
	 * 일정 수정
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param scheduleTsid 일정 TSID
	 * @param updateRequest 일정 수정 요청 DTO
	 * @return 수정된 일정 정보 (200 OK)
	 */
	@PutMapping("/{scheduleTsid}")
	public ResponseEntity<ScheduleResponse> updateSchedule(
		HttpServletRequest request,
		@PathVariable String scheduleTsid,
		@Valid @RequestBody UpdateScheduleRequest updateRequest) {

		String userTsid = authService.getCurrentUserTsid(request);
		ScheduleResponse response = scheduleService.updateSchedule(scheduleTsid, userTsid, updateRequest);

		return ResponseEntity.ok(response);
	}

	/**
	 * 일정 삭제
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param scheduleTsid 일정 TSID
	 * @return 204 No Content
	 */
	@DeleteMapping("/{scheduleTsid}")
	public ResponseEntity<Void> deleteSchedule(
		HttpServletRequest request,
		@PathVariable String scheduleTsid) {

		String userTsid = authService.getCurrentUserTsid(request);
		scheduleService.deleteSchedule(scheduleTsid, userTsid);

		return ResponseEntity.noContent().build();
	}
}
