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
import com.gathering.user.application.UserValidator;
import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;
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
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserValidator userValidator;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Test
	@DisplayName("회원 탈퇴 시 users, user_security 테이블에서 삭제되고 Redis 토큰이 삭제된다")
	void withdrawSuccess() {
		// given
		String tsid = "1234567890123";
		String password = "Password1!";
		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@example.com")
			.name("홍길동")
			.build();

		UserSecurityEntity security = UserSecurityEntity.of(tsid, passwordEncoder.encode(password));
		WithdrawRequest request = new WithdrawRequest(password);

		when(usersRepository.findById(tsid)).thenReturn(Optional.of(user));
		when(userSecurityRepository.findById(tsid)).thenReturn(Optional.of(security));
		when(passwordEncoder.matches(password, security.getPasswordHash())).thenReturn(true);
		when(refreshTokenService.deleteAllRefreshTokensByTsid(tsid)).thenReturn(1L);

		// when
		userService.withdraw(tsid, request);

		// then
		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, times(1)).findById(tsid);
		verify(passwordEncoder, times(1)).matches(password, security.getPasswordHash());
		verify(userSecurityRepository, times(1)).deleteById(tsid);
		verify(usersRepository, times(1)).deleteById(tsid);
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
		verify(userSecurityRepository, never()).deleteById(anyString());
		verify(usersRepository, never()).deleteById(anyString());
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 INVALID_CURRENT_PASSWORD 예외가 발생한다")
	void withdrawInvalidPassword() {
		// given
		String tsid = "1234567890123";
		String correctPassword = "Password1!";
		String wrongPassword = "WrongPass1!";

		UsersEntity user = UsersEntity.builder()
			.tsid(tsid)
			.email("test@example.com")
			.name("홍길동")
			.build();

		UserSecurityEntity security = UserSecurityEntity.of(tsid, passwordEncoder.encode(correctPassword));
		WithdrawRequest request = new WithdrawRequest(wrongPassword);

		when(usersRepository.findById(tsid)).thenReturn(Optional.of(user));
		when(userSecurityRepository.findById(tsid)).thenReturn(Optional.of(security));
		when(passwordEncoder.matches(wrongPassword, security.getPasswordHash())).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> userService.withdraw(tsid, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURRENT_PASSWORD);

		verify(usersRepository, times(1)).findById(tsid);
		verify(userSecurityRepository, times(1)).findById(tsid);
		verify(passwordEncoder, times(1)).matches(wrongPassword, security.getPasswordHash());
		verify(userSecurityRepository, never()).deleteById(anyString());
		verify(usersRepository, never()).deleteById(anyString());
		verify(refreshTokenService, never()).deleteAllRefreshTokensByTsid(anyString());
	}
}
