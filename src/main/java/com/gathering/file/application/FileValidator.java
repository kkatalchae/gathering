package com.gathering.file.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.domain.model.FileType;

@Component
public class FileValidator {

	private static final Detector DETECTOR = new DefaultDetector();

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
	private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

	public void validate(MultipartFile file, FileType fileType) {
		validateNotEmpty(file);
		validateExtension(file.getOriginalFilename());
		validateMimeType(file);
		validateSize(file, fileType);
	}

	private void validateNotEmpty(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.FILE_EMPTY);
		}
	}

	private void validateExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			throw new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
		}
		String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
		}
	}

	private void validateMimeType(MultipartFile file) {
		String detectedMimeType = detectMimeType(file);
		if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
			throw new BusinessException(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED);
		}
	}

	private String detectMimeType(MultipartFile file) {
		try (InputStream inputStream = file.getInputStream()) {
			Metadata metadata = new Metadata();
			MediaType mediaType = DETECTOR.detect(inputStream, metadata);
			return mediaType.toString();
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
		}
	}

	private void validateSize(MultipartFile file, FileType fileType) {
		if (file.getSize() > fileType.getMaxSize()) {
			throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
		}
	}
}
