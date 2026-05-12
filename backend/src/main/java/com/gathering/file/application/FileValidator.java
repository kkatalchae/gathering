package com.gathering.file.application;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.domain.model.FileType;

@Component
public class FileValidator {

	private static final Logger log = LoggerFactory.getLogger(FileValidator.class);

	private static final Detector DETECTOR = new DefaultDetector();

	static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
	static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

	public String validate(MultipartFile file, FileType fileType) {
		validateNotEmpty(file);
		validateExtension(file.getOriginalFilename());
		String detectedMimeType = validateMimeType(file);
		validateSize(file, fileType);
		return detectedMimeType;
	}

	void validateNotEmpty(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			log.warn("파일 검증 실패: 파일이 비어있음");
			throw new BusinessException(ErrorCode.FILE_EMPTY);
		}
	}

	void validateExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			log.warn("파일 검증 실패: 확장자 없음 - filename={}", filename);
			throw new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
		}
		String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			log.warn("파일 검증 실패: 허용되지 않는 확장자 - filename={}, extension={}, allowed={}", filename, extension, ALLOWED_EXTENSIONS);
			throw new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
		}
	}

	String validateMimeType(MultipartFile file) {
		if (file == null) {
			log.warn("파일 검증 실패: 파일이 null");
			throw new BusinessException(ErrorCode.FILE_EMPTY);
		}
		String detectedMimeType = detectMimeType(file);
		if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
			log.warn("파일 검증 실패: 허용되지 않는 MIME 타입 - filename={}, detectedMimeType={}, allowed={}", file.getOriginalFilename(), detectedMimeType, ALLOWED_MIME_TYPES);
			throw new BusinessException(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED);
		}
		return detectedMimeType;
	}

	private String detectMimeType(MultipartFile file) {
		try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
			Metadata metadata = new Metadata();
			MediaType mediaType = DETECTOR.detect(inputStream, metadata);
			return mediaType.toString();
		} catch (IOException e) {
			log.error("MIME 타입 감지 실패 - filename={}", file.getOriginalFilename(), e);
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
		}
	}

	void validateSize(MultipartFile file, FileType fileType) {
		if (file == null) {
			log.warn("파일 검증 실패: 파일이 null");
			throw new BusinessException(ErrorCode.FILE_EMPTY);
		}
		if (fileType == null) {
			log.warn("파일 검증 실패: fileType이 null - filename={}", file.getOriginalFilename());
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
		}
		if (fileType.getMaxSize() > 0 && file.getSize() > fileType.getMaxSize()) {
			log.warn("파일 검증 실패: 파일 크기 초과 - filename={}, size={}bytes, maxSize={}bytes, fileType={}", file.getOriginalFilename(), file.getSize(), fileType.getMaxSize(), fileType);
			throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
		}
	}
}
