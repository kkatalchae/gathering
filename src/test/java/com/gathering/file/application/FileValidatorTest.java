package com.gathering.file.application;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.domain.model.FileType;

@DisplayName("FileValidator 단위 테스트")
class FileValidatorTest {

	private final FileValidator fileValidator = new FileValidator();

	// MIME 타입별 매직 바이트. ALLOWED_MIME_TYPES 에 새 타입 추가 시 여기도 추가 필요
	private static final Map<String, byte[]> MIME_MAGIC_BYTES = Map.of(
		"image/jpeg", new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0, 0, 0, 0},
		"image/png", new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
		"image/gif", new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61},
		"image/webp", new byte[] {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50}
	);

	// ══════════════════════════════════════════════
	// validateNotEmpty
	// ══════════════════════════════════════════════

	@Nested
	@DisplayName("validateNotEmpty")
	class ValidateNotEmpty {

		@Test
		@DisplayName("null 파일 → FILE_EMPTY")
		void nullFile() {
			assertThatThrownBy(() -> fileValidator.validateNotEmpty(null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_EMPTY);
		}

		@Test
		@DisplayName("빈 파일 → FILE_EMPTY")
		void emptyFile() {
			MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
			assertThatThrownBy(() -> fileValidator.validateNotEmpty(file))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_EMPTY);
		}

		@Test
		@DisplayName("내용 있는 파일 → 예외 없음")
		void validFile() {
			MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[1024]);
			assertThatNoException().isThrownBy(() -> fileValidator.validateNotEmpty(file));
		}
	}

	// ══════════════════════════════════════════════
	// validateExtension
	// ══════════════════════════════════════════════

	@Nested
	@DisplayName("validateExtension")
	class ValidateExtension {

		// validator 내부에서 toLowerCase() 처리하므로 소문자로만 검증
		static Stream<String> allowedFilenames() {
			return FileValidator.ALLOWED_EXTENSIONS.stream()
				.map(ext -> "file." + ext);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("allowedFilenames")
		@DisplayName("허용된 확장자 → 예외 없음")
		void allowedExtensions(String filename) {
			assertThatNoException().isThrownBy(() -> fileValidator.validateExtension(filename));
		}

		@Test
		@DisplayName("대문자 확장자 → 예외 없음 (대소문자 구분 없음)")
		void uppercaseExtension() {
			assertThatNoException().isThrownBy(() -> fileValidator.validateExtension("PHOTO.JPG"));
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = {"malware.exe", "document.pdf", "script.js", "data.csv",
			"archive.zip", "program.sh", "config.xml", "filewithoutext"})
		@DisplayName("허용되지 않는 확장자 → FILE_EXTENSION_NOT_ALLOWED")
		void notAllowedExtensions(String filename) {
			assertThatThrownBy(() -> fileValidator.validateExtension(filename))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
		}

		@ParameterizedTest
		@NullSource
		@DisplayName("null 파일명 → FILE_EXTENSION_NOT_ALLOWED")
		void nullFilename(String filename) {
			assertThatThrownBy(() -> fileValidator.validateExtension(filename))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_EXTENSION_NOT_ALLOWED);
		}
	}

	// ══════════════════════════════════════════════
	// validateMimeType
	// ══════════════════════════════════════════════

	@Nested
	@DisplayName("validateMimeType")
	class ValidateMimeType {

		// ALLOWED_MIME_TYPES 를 순회하며 각 타입의 매직 바이트로 파일 생성
		static Stream<Arguments> allowedMimeFiles() {
			return FileValidator.ALLOWED_MIME_TYPES.stream()
				.map(mimeType -> {
					byte[] magic = MIME_MAGIC_BYTES.get(mimeType);
					byte[] content = Arrays.copyOf(magic, 1024);
					MultipartFile file = new MockMultipartFile("file", "test", mimeType, content);
					return Arguments.of(mimeType, file);
				});
		}

		@Test
		@DisplayName("null 파일 → FILE_EMPTY")
		void nullFile() {
			assertThatThrownBy(() -> fileValidator.validateMimeType(null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_EMPTY);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("allowedMimeFiles")
		@DisplayName("허용된 MIME 타입 → 예외 없음")
		void allowedMimeTypes(String mimeType, MultipartFile file) {
			assertThatNoException().isThrownBy(() -> fileValidator.validateMimeType(file));
		}

		@Test
		@DisplayName("확장자 위장 - .jpg 파일이지만 PHP 바이트 → FILE_MIME_TYPE_NOT_ALLOWED")
		void jpgExtensionWithPhpContent() {
			byte[] phpBytes = "<?php echo 'malicious'; ?>".getBytes();
			MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", phpBytes);
			assertThatThrownBy(() -> fileValidator.validateMimeType(file))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED);
		}

		@Test
		@DisplayName("확장자 위장 - .jpg 파일이지만 PDF 바이트 → FILE_MIME_TYPE_NOT_ALLOWED")
		void jpgExtensionWithPdfContent() {
			byte[] pdfBytes = "%PDF-1.4 fake pdf content".getBytes();
			MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", pdfBytes);
			assertThatThrownBy(() -> fileValidator.validateMimeType(file))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_MIME_TYPE_NOT_ALLOWED);
		}
	}

	// ══════════════════════════════════════════════
	// validateSize
	// ══════════════════════════════════════════════

	@Nested
	@DisplayName("validateSize")
	class ValidateSize {

		@Test
		@DisplayName("null 파일 → FILE_EMPTY")
		void nullFile() {
			assertThatThrownBy(() -> fileValidator.validateSize(null, FileType.PROFILE_IMAGE))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_EMPTY);
		}

		@Test
		@DisplayName("null fileType → FILE_UPLOAD_FAILED")
		void nullFileType() {
			MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[1024]);
			assertThatThrownBy(() -> fileValidator.validateSize(file, null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException)e).getErrorCode())
				.isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
		}

		// FileType.values() 전체 × 경계값 3가지 조합
		static Stream<Arguments> sizeBoundaryTestCases() {
			return Stream.of(FileType.values()).flatMap(fileType -> Stream.of(
				Arguments.of(fileType, fileType.getMaxSize() - 1, "maxSize-1", true),
				Arguments.of(fileType, fileType.getMaxSize(), "maxSize", true),
				Arguments.of(fileType, fileType.getMaxSize() + 1, "maxSize+1", false)
			));
		}

		@ParameterizedTest(name = "[{0}] {2}")
		@MethodSource("sizeBoundaryTestCases")
		@DisplayName("파일 크기 경계값")
		void sizeBoundary(FileType fileType, long size, String label, boolean shouldPass) {
			byte[] content = new byte[(int)size];
			MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", content);
			if (shouldPass) {
				assertThatNoException().isThrownBy(() -> fileValidator.validateSize(file, fileType));
			} else {
				assertThatThrownBy(() -> fileValidator.validateSize(file, fileType))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException)e).getErrorCode())
					.isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
			}
		}
	}
}
