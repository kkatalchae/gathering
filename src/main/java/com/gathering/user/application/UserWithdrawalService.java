package com.gathering.user.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.file.application.FileUploadService;
import com.gathering.file.domain.model.FileType;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.policy.UserWithdrawalHandler;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴 오케스트레이션
 * 도메인별 정리는 UserWithdrawalHandler 구현체들이 맡고, 이 서비스는 순서와 원자성만 책임진다:
 *   1. 모든 핸들러의 validateWithdrawable — 하나라도 거부하면 아무것도 바꾸지 않는다
 *   2. 모든 핸들러의 cleanUp (@Order 순)
 *   3. 사용자 row 익명화 + 인증 정보(비밀번호, 소셜 연동) 삭제
 * 새 도메인이 생기면 핸들러만 추가하면 되고 이 클래스는 바뀌지 않는다
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

	/** Spring 이 @Order 순으로 정렬해 주입한다 */
	private final List<UserWithdrawalHandler> handlers;
	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;
	private final UserOAuthConnectionRepository oauthConnectionRepository;
	private final FileUploadService fileUploadService;

	/**
	 * @param userTsid 탈퇴할 사용자 TSID (본인 확인은 호출 측에서 끝난 상태)
	 */
	@Transactional
	public void withdraw(String userTsid) {
		// 1. 거부 사유가 있으면 여기서 끝난다 — 어떤 정리도 시작하지 않는다
		handlers.forEach(handler -> handler.validateWithdrawable(userTsid));

		// 2. 도메인별 정리
		handlers.forEach(handler -> handler.cleanUp(userTsid));

		// 3. 개인정보 파기: 업로드한 프로필 이미지 파일, 인증 정보, 그리고 users row 익명화
		var profileImages = fileUploadService.findFilesByUploaderAndType(userTsid, FileType.PROFILE_IMAGE);
		if (!profileImages.isEmpty()) {
			fileUploadService.deleteFilesWithCleanup(profileImages);
		}
		oauthConnectionRepository.deleteByUserTsid(userTsid);
		userSecurityRepository.deleteById(userTsid);

		// 핸들러의 벌크 삭제(clearAutomatically)가 영속성 컨텍스트를 비우므로, 정리가 끝난 뒤 다시 로드해야
		// 익명화 변경이 dirty checking 에 잡힌다. 미리 로드한 엔티티는 이 시점에 detached 상태다
		UsersEntity user = usersRepository.findById(userTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		user.withdraw();
	}
}
