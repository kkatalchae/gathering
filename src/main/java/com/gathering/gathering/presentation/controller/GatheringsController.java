package com.gathering.gathering.presentation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.gathering.application.GatheringService;
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.presentation.dto.ChangeParticipantRoleRequest;
import com.gathering.gathering.presentation.dto.ChangeParticipantRoleResponse;
import com.gathering.gathering.presentation.dto.CreateGatheringRequest;
import com.gathering.gathering.presentation.dto.GatheringDetailResponse;
import com.gathering.gathering.presentation.dto.GatheringListRequest;
import com.gathering.gathering.presentation.dto.GatheringListResponse;
import com.gathering.gathering.presentation.dto.GatheringResponse;
import com.gathering.gathering.presentation.dto.UpdateGatheringRequest;

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
	public ResponseEntity<GatheringResponse> createGathering(
		HttpServletRequest request,
		@Valid @RequestBody CreateGatheringRequest createRequest) {

		String userTsid = authService.getCurrentUserTsid(request);
		GatheringResponse response = gatheringService.createGathering(userTsid, createRequest);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 모임 목록 조회
	 *
	 * @param categories 카테고리 필터 (다중 선택 가능)
	 * @param regionTsids 지역 TSID 필터 (다중 선택 가능)
	 * @param cursor 페이지 커서
	 * @param size 페이지 크기 (기본: 20, 최대: 100)
	 * @return 모임 목록 및 페이지네이션 정보
	 */
	@GetMapping
	public ResponseEntity<GatheringListResponse> getGatherings(
		@RequestParam(required = false) List<GatheringCategory> categories,
		@RequestParam(required = false) List<String> regionTsids,
		@RequestParam(required = false) String cursor,
		@RequestParam(defaultValue = "20") int size) {

		GatheringListRequest request = GatheringListRequest.builder()
			.categories(categories)
			.regionTsids(regionTsids)
			.cursor(cursor)
			.size(size)
			.build();

		request.validate();

		GatheringListResponse response = gatheringService.getGatherings(request);

		return ResponseEntity.ok(response);
	}

	/**
	 * 모임 상세 조회
	 *
	 * @param tsid 모임 TSID
	 * @return 모임 상세 정보
	 */
	@GetMapping("/{tsid}")
	public ResponseEntity<GatheringDetailResponse> getGatheringDetail(@PathVariable String tsid) {
		GatheringDetailResponse response = gatheringService.getGatheringDetail(tsid);

		return ResponseEntity.ok(response);
	}

	/**
	 * 모임 정보 수정
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param gatheringTsid 모임 TSID
	 * @param updateRequest 모임 수정 요청 DTO
	 * @return 수정된 모임 정보 (200 OK)
	 */
	@PutMapping("/{gatheringTsid}")
	public ResponseEntity<GatheringResponse> updateGathering(
		HttpServletRequest request,
		@PathVariable String gatheringTsid,
		@Valid @RequestBody UpdateGatheringRequest updateRequest) {

		String userTsid = authService.getCurrentUserTsid(request);
		GatheringResponse response = gatheringService.updateGathering(gatheringTsid, userTsid, updateRequest);

		return ResponseEntity.ok(response);
	}

	/**
	 * 모임 삭제
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param gatheringTsid 모임 TSID
	 * @return 204 No Content
	 */
	@DeleteMapping("/{gatheringTsid}")
	public ResponseEntity<Void> deleteGathering(
		HttpServletRequest request,
		@PathVariable String gatheringTsid) {

		String userTsid = authService.getCurrentUserTsid(request);
		gatheringService.deleteGathering(gatheringTsid, userTsid);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 참여자 역할 변경
	 *
	 * @param request HTTP 요청 (인증 정보 추출용)
	 * @param gatheringTsid 모임 TSID
	 * @param targetUserTsid 대상 사용자 TSID
	 * @param changeRoleRequest 역할 변경 요청 DTO
	 * @return 역할 변경 결과 (200 OK)
	 */
	@PatchMapping("/{gatheringTsid}/participants/{targetUserTsid}/role")
	public ResponseEntity<ChangeParticipantRoleResponse> changeParticipantRole(
		HttpServletRequest request,
		@PathVariable String gatheringTsid,
		@PathVariable String targetUserTsid,
		@Valid @RequestBody ChangeParticipantRoleRequest changeRoleRequest) {

		String requesterTsid = authService.getCurrentUserTsid(request);
		ChangeParticipantRoleResponse response = gatheringService.changeParticipantRole(
			gatheringTsid,
			requesterTsid,
			targetUserTsid,
			changeRoleRequest
		);

		return ResponseEntity.ok(response);
	}
}
