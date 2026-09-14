package com.gathering.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gathering.auth.application.RefreshTokenService;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.application.UserService;
import com.gathering.user.application.UserWithdrawalService;
import com.gathering.user.application.UserValidator;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserOAuthConnectionRepository;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;
import com.gathering.user.presentation.dto.WithdrawRequest;

/**
 * UserService 비즈니스 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UsersRepository usersRepository;

	@Mock
	private UserSecurityRepository userSecurityRepository;

	@Mock
	private UserOAuthConnectionRepository oauthConnectionRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserValidator userValidator;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private UserWithdrawalService userWithdrawalService;

	@Test
	@DisplayName("회원 탈퇴 시 본인 확인 후 연관 데이터 정리·익명화를 위임하고 Redis 토큰이 삭제된다")
	void withdrawSuccess() {
		// given
		String tsid = "1234567890123";
		String password = "Password1!";
		String encodedPassword = "$2a$10$encoded_password";

		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@example.com")
			.name("홍길동")
			.build();

		UserSecurityEntity security = UserSecurityEntity.of(tsid, encodedPassword);
		WithdrawRequest request = new WithdrawRequest(password);

		when(usersRepository.findById(tsid)).thenReturn(Optional.of(user));
		when(userSecurityRepository.findById(tsid)).thenReturn(Optional.of(security));
		when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
		when(refreshTokenService.deleteAllRefreshTokensByTsid(tsid)).thenReturn(1L);

		// when
		userService.withdraw(tsid, request);

		// then
		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, times(1)).findById(tsid);
		verify(passwordEncoder, times(1)).matches(password, encodedPassword);
		// 연관 데이터 정리와 익명화는 UserWithdrawalService 가 맡는다 (하드 삭제하지 않는다)
		verify(userWithdrawalService, times(1)).withdraw(tsid);
		verify(usersRepository, never()).deleteById(anyString());
		// 세션에 대한 부분도 삭제되는가?
		verify(refreshTokenService, times(1)).deleteAllRefreshTokensByTsid(tsid);
	}

	@Test
	@DisplayName("존재하지 않는 사용자 탈퇴 시 USER_NOT_FOUND 예외가 발생한다")
	void withdrawUserNotFound() {
		// given
		String tsid = "1234567890123";
		WithdrawRequest request = new WithdrawRequest("Password1!");

		when(usersRepository.findById(tsid)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.withdraw(tsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, never()).findById(anyString());
		verify(refreshTokenService, never()).deleteAllRefreshTokensByTsid(anyString());
		verify(userWithdrawalService, never()).withdraw(any());
	}

	@Test
	@DisplayName("이미 탈퇴한 사용자는 USER_DELETED 로 조회가 거부된다")
	void getUsersEntityByTsidRejectsWithdrawn() {
		// given
		String tsid = "1234567890123";
		UsersEntity withdrawn = UsersEntity.builder().tsid(tsid).email("x@example.com").name("x").build();
		withdrawn.withdraw();
		when(usersRepository.findById(tsid)).thenReturn(Optional.of(withdrawn));

		// when & then
		assertThatThrownBy(() -> userService.getUsersEntityByTsid(tsid))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_DELETED);
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 INVALID_CURRENT_PASSWORD 예외가 발생한다")
	void withdrawInvalidPassword() {
		// given
		String tsid = "1234567890123";
		String wrongPassword = "WrongPass1!";
		String encodedPassword = "$2a$10$encoded_password";

		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@example.com")
			.name("홍길동")
			.build();

		UserSecurityEntity security = UserSecurityEntity.of(tsid, encodedPassword);
		WithdrawRequest request = new WithdrawRequest(wrongPassword);

		when(usersRepository.findById(tsid)).thenReturn(Optional.of(user));
		when(userSecurityRepository.findById(tsid)).thenReturn(Optional.of(security));
		when(passwordEncoder.matches(wrongPassword, encodedPassword)).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> userService.withdraw(tsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURRENT_PASSWORD);

		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, times(1)).findById(tsid);
		verify(passwordEncoder, times(1)).matches(wrongPassword, encodedPassword);
		verify(userSecurityRepository, never()).deleteById(anyString());
		verify(usersRepository, never()).deleteById(anyString());
		verify(refreshTokenService, never()).deleteAllRefreshTokensByTsid(anyString());
	}

	@Test
	@DisplayName("소셜 로그인 사용자는 비밀번호 없이 탈퇴가 가능하다")
	void withdrawSocialUser() {
		// given
		String tsid = "1234567890123";
		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("social@example.com")
			.name("구글사용자")
			.build();

		// 소셜 로그인 사용자는 passwordHash가 null
		UserSecurityEntity security = UserSecurityEntity.of(tsid, null);
		WithdrawRequest request = new WithdrawRequest(null);

		when(usersRepository.findById(tsid)).thenReturn(Optional.of(user));
		when(userSecurityRepository.findById(tsid)).thenReturn(Optional.of(security));
		when(refreshTokenService.deleteAllRefreshTokensByTsid(tsid)).thenReturn(1L);

		// when
		userService.withdraw(tsid, request);

		// then
		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, times(1)).findById(tsid);
		verify(passwordEncoder, never()).matches(anyString(), anyString());
		verify(userWithdrawalService, times(1)).withdraw(tsid);
		verify(refreshTokenService, times(1)).deleteAllRefreshTokensByTsid(tsid);
	}

	@Test
	@DisplayName("일반 회원가입 사용자가 비밀번호 없이 탈퇴 시도하면 INVALID_CURRENT_PASSWORD 예외가 발생한다")
	void withdrawRegularUserWithoutPassword() {
		// given
		String tsid = "1234567890123";
		String encodedPassword = "$2a$10$encoded_password";

		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@example.com")
			.name("홍길동")
			.build();

		UserSecurityEntity security = UserSecurityEntity.of(tsid, encodedPassword);
		WithdrawRequest request = new WithdrawRequest(null);

		when(usersRepository.findById(tsid)).thenReturn(Optional.of(user));
		when(userSecurityRepository.findById(tsid)).thenReturn(Optional.of(security));

		// when & then
		assertThatThrownBy(() -> userService.withdraw(tsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURRENT_PASSWORD);

		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, times(1)).findById(tsid);
		verify(passwordEncoder, never()).matches(anyString(), anyString());
		verify(userSecurityRepository, never()).deleteById(anyString());
		verify(usersRepository, never()).deleteById(anyString());
		verify(refreshTokenService, never()).deleteAllRefreshTokensByTsid(anyString());
	}
}
