package com.gathering.file;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.application.FileValidator;
import com.gathering.file.domain.model.FileType;

@DisplayName("FileValidator 단위 테스트")
class FileValidatorTest {

	private final FileValidator fileValidator = new FileValidator();

	// ────────────── 정상 케이스 ──────────────

	@Test
	@DisplayName("정상 JPEG 파일 (2MB) - 예외 없음")
	void validate_validJpeg_noException() throws IOException {
		MultipartFile file = createFakeJpegFile("photo.jpg", 2 * 1024 * 1024);
		assertThatNoException().isThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE));
	}

	@Test
	@DisplayName("정상 PNG 파일 (1MB) - 예외 없음")
	void validate_validPng_noException() throws IOException {
		MultipartFile file = createFakePngFile("image.png", 1024 * 1024);
		assertThatNoException().isThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE));
	}

	@Test
	@DisplayName("정상 WebP 파일 (3MB) - 예외 없음")
	void validate_validWebp_noException() throws IOException {
		MultipartFile file = createFakeWebpFile("image.webp", 3 * 1024 * 1024);
		assertThatNoException().isThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE));
	}

	@Test
	@DisplayName("대문자 확장자 (PHOTO.JPG) - 예외 없음")
	void validate_uppercaseExtension_noException() throws IOException {
		MultipartFile file = createFakeJpegFile("PHOTO.JPG", 1024 * 1024);
		assertThatNoException().isThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE));
	}

	@Test
	@DisplayName("모임 이미지 7MB - 프로필 제한(5MB) 초과지만 모임 이미지 제한(10MB) 이내 - 예외 없음")
	void validate_gatheringImage7MB_noException() throws IOException {
		MultipartFile file = createFakeJpegFile("gathering.jpg", 7 * 1024 * 1024);
		assertThatNoException().isThrownBy(() -> fileValidator.validate(file, FileType.GATHERING_IMAGE));
	}

	// ────────────── 파일 비어있음 ──────────────

	@Test
	@DisplayName("null 파일 - FILE_EMPTY 예외")
	void validate_nullFile_throwsFileEmpty() {
		assertThatThrownBy(() -> fileValidator.validate(null, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_EMPTY);
	}

	@Test
	@DisplayName("빈 파일 (isEmpty=true) - FILE_EMPTY 예외")
	void validate_emptyFile_throwsFileEmpty() {
		MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_EMPTY);
	}

	// ────────────── 확장자 검사 ──────────────

	@Test
	@DisplayName("허용되지 않은 확장자 .exe - FILE_EXTENSION_NOT_ALLOWED 예외")
	void validate_exeExtension_throwsExtensionNotAllowed() {
		MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream",
			new byte[1024]);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
	}

	@Test
	@DisplayName("허용되지 않은 확장자 .pdf - FILE_EXTENSION_NOT_ALLOWED 예외")
	void validate_pdfExtension_throwsExtensionNotAllowed() {
		MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[1024]);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
	}

	@Test
	@DisplayName("확장자 없는 파일 - FILE_EXTENSION_NOT_ALLOWED 예외")
	void validate_noExtension_throwsExtensionNotAllowed() {
		MockMultipartFile file = new MockMultipartFile("file", "filewithoutext", "image/jpeg", new byte[1024]);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
	}

	// ────────────── MIME 타입 위장 공격 ──────────────

	@Test
	@DisplayName("확장자 위장 공격 - .jpg 파일이지만 PHP 바이트 - FILE_MIME_TYPE_NOT_ALLOWED 예외")
	void validate_jpgExtensionWithPhpContent_throwsMimeNotAllowed() {
		// PHP 파일 시작 바이트 (<?php)
		byte[] phpBytes = "<?php echo 'malicious'; ?>".getBytes();
		MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", phpBytes);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED);
	}

	@Test
	@DisplayName("확장자 위장 공격 - .jpg 파일이지만 PDF 바이트 - FILE_MIME_TYPE_NOT_ALLOWED 예외")
	void validate_jpgExtensionWithPdfContent_throwsMimeNotAllowed() {
		// PDF 파일 시작 바이트 (%PDF-)
		byte[] pdfBytes = "%PDF-1.4 fake pdf content".getBytes();
		MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", pdfBytes);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED);
	}

	// ────────────── 파일 크기 검사 ──────────────

	@Test
	@DisplayName("프로필 이미지 5MB 초과 (6MB) - FILE_SIZE_EXCEEDED 예외")
	void validate_profileImage6MB_throwsSizeExceeded() throws IOException {
		MultipartFile file = createFakeJpegFile("big.jpg", 6 * 1024 * 1024);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.PROFILE_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
	}

	@Test
	@DisplayName("모임 이미지 10MB 초과 (11MB) - FILE_SIZE_EXCEEDED 예외")
	void validate_gatheringImage11MB_throwsSizeExceeded() throws IOException {
		MultipartFile file = createFakeJpegFile("huge.jpg", 11 * 1024 * 1024);
		assertThatThrownBy(() -> fileValidator.validate(file, FileType.GATHERING_IMAGE))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
	}

	// ────────────── 헬퍼 메서드 ──────────────

	/**
	 * 실제 JPEG 매직 바이트(FF D8 FF)로 시작하는 가짜 JPEG 파일 생성
	 */
	private MultipartFile createFakeJpegFile(String filename, int size) {
		byte[] content = new byte[size];
		// JPEG 매직 바이트
		content[0] = (byte)0xFF;
		content[1] = (byte)0xD8;
		content[2] = (byte)0xFF;
		content[3] = (byte)0xE0;
		return new MockMultipartFile("file", filename, "image/jpeg", content);
	}

	/**
	 * 실제 PNG 매직 바이트(89 50 4E 47)로 시작하는 가짜 PNG 파일 생성
	 */
	private MultipartFile createFakePngFile(String filename, int size) {
		byte[] content = new byte[size];
		// PNG 매직 바이트
		content[0] = (byte)0x89;
		content[1] = (byte)0x50; // P
		content[2] = (byte)0x4E; // N
		content[3] = (byte)0x47; // G
		content[4] = (byte)0x0D;
		content[5] = (byte)0x0A;
		content[6] = (byte)0x1A;
		content[7] = (byte)0x0A;
		return new MockMultipartFile("file", filename, "image/png", content);
	}

	/**
	 * WebP 파일은 RIFF....WEBP 형식으로 시작
	 */
	private MultipartFile createFakeWebpFile(String filename, int size) {
		byte[] content = new byte[Math.max(size, 12)];
		// RIFF 헤더
		content[0] = (byte)0x52; // R
		content[1] = (byte)0x49; // I
		content[2] = (byte)0x46; // F
		content[3] = (byte)0x46; // F
		// 파일 크기 (4바이트, little endian - 임의 값)
		content[4] = (byte)(size & 0xFF);
		content[5] = (byte)((size >> 8) & 0xFF);
		content[6] = (byte)((size >> 16) & 0xFF);
		content[7] = (byte)((size >> 24) & 0xFF);
		// WEBP 마커
		content[8] = (byte)0x57;  // W
		content[9] = (byte)0x45;  // E
		content[10] = (byte)0x42; // B
		content[11] = (byte)0x50; // P
		return new MockMultipartFile("file", filename, "image/webp", content);
	}
}
