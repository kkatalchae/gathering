package com.gathering.gathering.presentation.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.auth.application.AuthService;
import com.gathering.file.presentation.dto.FileUploadResponse;
import com.gathering.gathering.application.GatheringService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequestMapping("/gatherings")
@RestController
@RequiredArgsConstructor
public class GatheringController {

	private final GatheringService gatheringService;
	private final AuthService authService;

	/**
	 * 모임 대표 이미지 업로드
	 * OWNER 권한을 가진 사용자만 업로드 가능
	 */
	@PatchMapping(value = "/{tsid}/main-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileUploadResponse> uploadMainImage(
		@PathVariable String tsid,
		HttpServletRequest request,
		@RequestPart("file") MultipartFile file) {
		String requesterTsid = authService.getCurrentUserTsid(request);
		String url = gatheringService.uploadMainImage(tsid, requesterTsid, file);
		return ResponseEntity.ok(new FileUploadResponse(url));
	}
}
