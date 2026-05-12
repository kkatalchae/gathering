package com.gathering.file.infra;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.file.application.StorageResult;
import com.gathering.file.application.StorageService;

import lombok.extern.slf4j.Slf4j;

/**
 * S3 기반 파일 저장소 (운영 환경)
 * TODO: AWS SDK v2를 사용하여 실제 S3 업로드 구현 필요
 *       - CloudFront OAC(Origin Access Control)를 통한 파일 서빙
 *       - 버킷 정책: public 접근 차단, CloudFront OAC만 허용
 */
@Slf4j
@Service
@Profile("prod")
public class S3StorageService implements StorageService {

	@Override
	public StorageResult store(MultipartFile file, String directory) {
		// TODO: AWS SDK v2 S3Client 구현
		log.warn("S3StorageService.store() 는 아직 구현되지 않았습니다. directory={}", directory);
		throw new UnsupportedOperationException("S3 업로드는 아직 구현되지 않았습니다.");
	}

	@Override
	public void delete(String storagePath) {
		// TODO: AWS SDK v2 S3Client deleteObject 구현
		log.warn("S3StorageService.delete() 는 아직 구현되지 않았습니다. storagePath={}", storagePath);
	}
}
