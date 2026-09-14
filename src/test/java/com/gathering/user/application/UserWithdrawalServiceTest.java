package com.gathering.user.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.file.application.FileUploadService;
import com.gathering.file.domain.model.FileMetadataEntity;
import com.gathering.file.domain.model.FileType;
import com.gathering.user.domain.model.UserStatus;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.policy.UserWithdrawalHandler;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * UserWithdrawalService 테스트 — 핸들러 호출 순서와 원자성(거부 시 아무것도 바꾸지 않음)을 검증한다
 */
@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

	private static final String USER_TSID = "01HQUSER000001";

	@Mock
	private UserWithdrawalHandler firstHandler;

	@Mock
	private UserWithdrawalHandler secondHandler;

	@Mock
	private UsersRepository usersRepository;

	@Mock
	private UserSecurityRepository userSecurityRepository;

	@Mock
	private UserOAuthConnectionRepository oauthConnectionRepository;

	@Mock
	private FileUploadService fileUploadService;

	private UserWithdrawalService service() {
		return new UserWithdrawalService(List.of(firstHandler, secondHandler), usersRepository,
			userSecurityRepository, oauthConnectionRepository, fileUploadService);
	}

	private UsersEntity activeUser() {
		return UsersEntity.builder().tsid(USER_TSID).email("kim@example.com").name("김병채").build();
	}

	@Test
	@DisplayName("모든 핸들러의 검증을 먼저 끝낸 뒤 등록 순서대로 정리하고 마지막에 익명화한다")
	void validatesAllThenCleansUpInOrderThenAnonymizes() {
		// given
		UsersEntity user = activeUser();
		given(fileUploadService.findFilesByUploaderAndType(USER_TSID, FileType.PROFILE_IMAGE)).willReturn(List.of());
		given(usersRepository.findById(USER_TSID)).willReturn(Optional.of(user));

		// when
		service().withdraw(USER_TSID);

		// then
		InOrder order = inOrder(firstHandler, secondHandler, oauthConnectionRepository, userSecurityRepository,
			usersRepository);
		order.verify(firstHandler).validateWithdrawable(USER_TSID);
		order.verify(secondHandler).validateWithdrawable(USER_TSID);
		order.verify(firstHandler).cleanUp(USER_TSID);
		order.verify(secondHandler).cleanUp(USER_TSID);
		order.verify(oauthConnectionRepository).deleteByUserTsid(USER_TSID);
		order.verify(userSecurityRepository).deleteById(USER_TSID);
		// 정리(벌크 삭제로 컨텍스트가 비워짐) 이후에 다시 로드해 익명화해야 변경이 저장된다
		order.verify(usersRepository).findById(USER_TSID);
		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	@DisplayName("핸들러 하나가 거부하면 어떤 정리도 시작하지 않고 사용자도 그대로다")
	void rejectionStopsEverything() {
		// given: 두 번째 핸들러가 거부 — 첫 번째 핸들러의 정리도 실행되면 안 된다
		UsersEntity user = activeUser();
		willThrow(new BusinessException(ErrorCode.CANNOT_WITHDRAW_AS_GATHERING_OWNER))
			.given(secondHandler).validateWithdrawable(USER_TSID);

		// when & then
		assertThatThrownBy(() -> service().withdraw(USER_TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_WITHDRAW_AS_GATHERING_OWNER);

		then(firstHandler).should(never()).cleanUp(any());
		then(secondHandler).should(never()).cleanUp(any());
		then(oauthConnectionRepository).shouldHaveNoInteractions();
		then(userSecurityRepository).shouldHaveNoInteractions();
		then(fileUploadService).shouldHaveNoInteractions();
		then(usersRepository).shouldHaveNoInteractions();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getEmail()).isEqualTo("kim@example.com");
	}

	@Test
	@DisplayName("업로드한 프로필 이미지 파일이 있으면 함께 삭제한다")
	void deletesProfileImageFiles() {
		// given
		UsersEntity user = activeUser();
		List<FileMetadataEntity> metadata = List.of(mock(FileMetadataEntity.class));
		given(fileUploadService.findFilesByUploaderAndType(USER_TSID, FileType.PROFILE_IMAGE)).willReturn(metadata);
		given(usersRepository.findById(USER_TSID)).willReturn(Optional.of(user));

		// when
		service().withdraw(USER_TSID);

		// then
		then(fileUploadService).should().deleteFilesWithCleanup(metadata);
	}
}
