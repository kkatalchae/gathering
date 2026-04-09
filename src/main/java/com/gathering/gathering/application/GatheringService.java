package com.gathering.gathering.application;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.application.FileUploadService;
import com.gathering.file.domain.model.FileType;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GatheringService {

	private final GatheringRepository gatheringRepository;
	private final GatheringParticipantRepository gatheringParticipantRepository;
	private final FileUploadService fileUploadService;

	/**
	 * 모임 대표 이미지 업로드
	 * OWNER 권한이 있는 사용자만 업로드 가능
	 *
	 * @param gatheringTsid 모임 TSID
	 * @param requesterTsid 요청자 TSID
	 * @param file 업로드할 이미지 파일
	 * @return 업로드된 파일의 공개 URL
	 */
	@Transactional
	public String uploadMainImage(String gatheringTsid, String requesterTsid, MultipartFile file) {
		GatheringEntity gathering = gatheringRepository.findById(gatheringTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_NOT_FOUND));

		gatheringParticipantRepository.findByGatheringTsidAndUserTsidAndRole(
				gatheringTsid, requesterTsid, ParticipantRole.OWNER)
			.orElseThrow(() -> new BusinessException(ErrorCode.GATHERING_ACCESS_DENIED));

		String url = fileUploadService.upload(file, FileType.GATHERING_IMAGE, requesterTsid);
		gathering.updateMainImageUrl(url);
		return url;
	}
}
