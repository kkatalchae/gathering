package com.gathering.gathering.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.gathering.application.GatheringService;
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.CreateGatheringResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 모임 API Controller
 */
@RestController
@RequestMapping("/gatherings")
@RequiredArgsConstructor
public class GatheringsController {

	private final GatheringService gatheringService;
	private final AuthService authService;

	/**
	 * 모임 생성
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param createRequest 모임 생성 요청 DTO
	 * @return 생성된 모임 정보 (201 Created)
	 */
	@PostMapping
	public ResponseEntity<CreateGatheringResponse> createGathering(
		HttpServletRequest request,
		@Valid @RequestBody CreateGatheringRequest createRequest) {

		String userTsid = authService.getCurrentUserTsid(request);
		CreateGatheringResponse response = gatheringService.createGathering(userTsid, createRequest);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}