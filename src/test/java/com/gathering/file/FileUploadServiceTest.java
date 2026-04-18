package com.gathering.file;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.application.FileUploadService;
import com.gathering.file.application.FileValidator;
import com.gathering.file.application.StorageResult;
import com.gathering.file.application.StorageService;
import com.gathering.file.domain.model.FileMetadataEntity;
import com.gathering.file.domain.model.FileType;
import com.gathering.file.domain.repository.FileMetadataRepository;

@DisplayName("FileUploadService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

	@InjectMocks
	private FileUploadService fileUploadService;

	@Mock
	private FileValidator fileValidator;

	@Mock
	private StorageService storageService;

	@Mock
	private FileMetadataRepository fileMetadataRepository;

	@Test
	@DisplayName("성공적인 업로드 - URL 반환, 메타데이터 저장")
	void upload_success_returnsUrlAndSavesMetadata() {
		// given
		MultipartFile file = createValidJpegFile();
		String uploaderTsid = "testUserTsid00";
		StorageResult storageResult = new StorageResult(
			"/uploads/profile-images/uuid.jpg",
			"profile-images/uuid.jpg",
			"uuid.jpg"
		);

		willDoNothing().given(fileValidator).validate(any(), any());
		given(storageService.store(any(), anyString())).willReturn(storageResult);
		given(fileMetadataRepository.save(any(FileMetadataEntity.class))).willAnswer(i -> i.getArgument(0));

		// when
		String resultUrl = fileUploadService.upload(file, FileType.PROFILE_IMAGE, uploaderTsid);

		// then
		assertThat(resultUrl).isEqualTo("/uploads/profile-images/uuid.jpg");
		then(fileMetadataRepository).should().save(any(FileMetadataEntity.class));
	}

	@Test
	@DisplayName("저장소 쓰기 실패 - FILE_UPLOAD_FAILED 예외, 메타데이터 미저장")
	void upload_storageFailure_throwsFileUploadFailed() {
		// given
		MultipartFile file = createValidJpegFile();
		willDoNothing().given(fileValidator).validate(any(), any());
		given(storageService.store(any(), anyString()))
			.willThrow(new BusinessException(ErrorCode.FILE_UPLOAD_FAILED));

		// when & then
		assertThatThrownBy(() -> fileUploadService.upload(file, FileType.PROFILE_IMAGE, "uploaderTsid"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

		then(fileMetadataRepository).should(never()).save(any());
	}

	private MultipartFile createValidJpegFile() {
		byte[] content = new byte[1024];
		content[0] = (byte)0xFF;
		content[1] = (byte)0xD8;
		content[2] = (byte)0xFF;
		return new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
	}
}
