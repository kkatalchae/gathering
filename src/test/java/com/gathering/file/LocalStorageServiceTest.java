package com.gathering.file;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.file.application.StorageResult;
import com.gathering.file.infra.FileUploadConfig;
import com.gathering.file.infra.LocalStorageService;

@DisplayName("LocalStorageService 단위 테스트")
class LocalStorageServiceTest {

	@TempDir
	Path tempDir;

	private LocalStorageService localStorageService;

	@BeforeEach
	void setUp() {
		FileUploadConfig config = new FileUploadConfig();
		config.setLocalUploadPath(tempDir.toString());
		config.setUrlPrefix("/uploads");
		localStorageService = new LocalStorageService(config);
	}

	@Test
	@DisplayName("파일 저장 - UUID 이름으로 디렉토리에 파일이 생성된다")
	void store_validFile_createsFileOnDisk() throws IOException {
		byte[] content = createJpegBytes();
		MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

		StorageResult result = localStorageService.store(file, "profile-images");

		Path expectedDir = tempDir.resolve("profile-images");
		assertThat(expectedDir).isDirectory();
		assertThat(result.storedFilename()).endsWith(".jpg");
		assertThat(result.publicUrl()).startsWith("/uploads/profile-images/");
		assertThat(result.storagePath()).startsWith("profile-images/");

		// 실제 파일이 존재하는지 확인
		Path storedFile = tempDir.resolve(result.storagePath());
		assertThat(storedFile).exists();
	}

	@Test
	@DisplayName("파일 저장 - 저장된 파일의 내용이 원본과 동일하다")
	void store_validFile_contentMatches() throws IOException {
		byte[] content = createJpegBytes();
		MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

		StorageResult result = localStorageService.store(file, "profile-images");
		Path storedFile = tempDir.resolve(result.storagePath());

		byte[] storedContent = Files.readAllBytes(storedFile);
		assertThat(storedContent).isEqualTo(content);
	}

	@Test
	@DisplayName("파일 삭제 - 저장된 파일이 디스크에서 제거된다")
	void delete_existingFile_removesFromDisk() throws IOException {
		byte[] content = createJpegBytes();
		MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
		StorageResult result = localStorageService.store(file, "profile-images");

		Path storedFile = tempDir.resolve(result.storagePath());
		assertThat(storedFile).exists();

		localStorageService.delete(result.storagePath());

		assertThat(storedFile).doesNotExist();
	}

	@Test
	@DisplayName("존재하지 않는 파일 삭제 - 예외 없이 처리된다 (idempotent)")
	void delete_nonExistentFile_noException() {
		assertThatNoException().isThrownBy(() -> localStorageService.delete("profile-images/nonexistent.jpg"));
	}

	@Test
	@DisplayName("저장 디렉토리가 없을 때 - 자동으로 생성된다")
	void store_directoryNotExists_createsDirectory() throws IOException {
		byte[] content = createJpegBytes();
		MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

		// 존재하지 않는 하위 디렉토리 지정
		localStorageService.store(file, "new-dir/sub-dir");

		Path newDir = tempDir.resolve("new-dir/sub-dir");
		assertThat(newDir).isDirectory();
	}

	private byte[] createJpegBytes() {
		byte[] content = new byte[1024];
		content[0] = (byte)0xFF;
		content[1] = (byte)0xD8;
		content[2] = (byte)0xFF;
		content[3] = (byte)0xE0;
		return content;
	}
}
