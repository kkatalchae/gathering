package com.gathering.file.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.file.domain.model.FileMetadataEntity;
import com.gathering.file.domain.model.FileType;
import com.gathering.file.domain.repository.FileMetadataRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileUploadService {

	private final FileValidator fileValidator;
	private final StorageService storageService;
	private final FileMetadataRepository fileMetadataRepository;

	/**
	 * 파일 업로드 처리
	 * 1. 파일 유효성 검사
	 * 2. 저장소에 파일 저장
	 * 3. 트랜잭션 롤백 시 파일 삭제 콜백 등록
	 * 4. 메타데이터 DB 저장
	 *
	 * @param file 업로드할 파일
	 * @param fileType 파일 타입 (크기 제한 기준)
	 * @param uploaderTsid 업로더 사용자 TSID
	 * @return 업로드된 파일의 공개 URL
	 */
	@Transactional
	public String upload(MultipartFile file, FileType fileType, String uploaderTsid) {
		fileValidator.validate(file, fileType);

		String directory = resolveDirectory(fileType);
		StorageResult storageResult = storageService.store(file, directory);

		// 트랜잭션 롤백 시 저장된 파일 삭제
		registerRollbackCleanup(storageResult.storagePath());

		FileMetadataEntity metadata = FileMetadataEntity.builder()
			.originalFilename(file.getOriginalFilename())
			.storedFilename(storageResult.storedFilename())
			.storagePath(storageResult.storagePath())
			.mimeType(file.getContentType())
			.fileSize(file.getSize())
			.uploaderTsid(uploaderTsid)
			.fileType(fileType)
			.build();

		fileMetadataRepository.save(metadata);

		return storageResult.publicUrl();
	}

	private void registerRollbackCleanup(String storagePath) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status == STATUS_ROLLED_BACK) {
						storageService.delete(storagePath);
					}
				}
			});
		}
	}

	private String resolveDirectory(FileType fileType) {
		return switch (fileType) {
			case PROFILE_IMAGE -> "profile-images";
			case GATHERING_IMAGE -> "gathering-images";
		};
	}
}
