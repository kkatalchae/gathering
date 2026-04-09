package com.gathering.file.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.application.StorageResult;
import com.gathering.file.application.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile({"default", "test"})
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

	private final FileUploadConfig config;

	@Override
	public StorageResult store(MultipartFile file, String directory) {
		String originalFilename = file.getOriginalFilename();
		String extension = extractExtension(originalFilename);
		String storedFilename = UUID.randomUUID() + "." + extension;
		String storagePath = directory + "/" + storedFilename;

		Path uploadDir = Paths.get(config.getLocalUploadPath(), directory);
		try {
			Files.createDirectories(uploadDir);
			Path targetPath = uploadDir.resolve(storedFilename);
			file.transferTo(targetPath.toFile());
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e);
		}

		String publicUrl = config.getUrlPrefix() + "/" + storagePath;
		return new StorageResult(publicUrl, storagePath, storedFilename);
	}

	@Override
	public void delete(String storagePath) {
		Path filePath = Paths.get(config.getLocalUploadPath(), storagePath);
		try {
			Files.deleteIfExists(filePath);
		} catch (IOException e) {
			log.warn("파일 삭제 실패: {}", storagePath, e);
		}
	}

	private String extractExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
	}
}
