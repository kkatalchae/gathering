package com.gathering.auth.infra;

import static org.mockito.Mockito.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthFailureHandler 테스트")
class OAuthFailureHandlerTest {

	@InjectMocks
	private OAuthFailureHandler oAuthFailureHandler;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@BeforeEach
	void setUp() {
		// Mock 객체는 @Mock 어노테이션으로 자동 생성됨
	}

	@Test
	@DisplayName("일반_OAuth_인증_실패_시_기본_에러_메시지로_리다이렉트한다")
	void 일반_OAuth_인증_실패_시_기본_에러_메시지로_리다이렉트한다() throws Exception {
		// ===== Given: 일반적인 OAuth 인증 실패 상황 (cause 없음) =====
		// AuthenticationException만 발생하고 특별한 원인(cause)이 없는 경우
		AuthenticationException exception = new AuthenticationException("oauth_error") {
		};

		// ===== When: OAuthFailureHandler 실행 =====
		// onAuthenticationFailure() 메서드 내부에서:
		// 1. exception.getCause()를 확인 -> null이므로 기본 에러 메시지 사용
		// 2. 기본 에러 메시지: "소셜 로그인에 실패했습니다."
		// 3. 에러 메시지를 URL 인코딩
		// 4. /login 페이지로 리다이렉트 (에러 정보 포함)
		oAuthFailureHandler.onAuthenticationFailure(request, response, exception);

		// ===== Then: 기본 에러 메시지로 리다이렉트되었는지 검증 =====
		String defaultErrorMessage = "소셜 로그인에 실패했습니다.";
		String expectedRedirectUrl = "/login?error=oauth_failed&message="
			+ URLEncoder.encode(defaultErrorMessage, StandardCharsets.UTF_8);

		verify(response, times(1)).sendRedirect(expectedRedirectUrl);
	}

	@Test
	@DisplayName("BusinessException이_원인인_경우_상세_에러_메시지로_리다이렉트한다")
	void BusinessException이_원인인_경우_상세_에러_메시지로_리다이렉트한다() throws Exception {
		// ===== Given: BusinessException이 원인인 OAuth 인증 실패 =====
		// 예: 이미 가입된 이메일로 OAuth 로그인 시도
		BusinessException cause = new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);

		// AuthenticationException의 cause로 BusinessException 설정
		AuthenticationException exception = new AuthenticationException("oauth_error", cause) {
		};

		// ===== When: OAuthFailureHandler 실행 =====
		// onAuthenticationFailure() 메서드 내부에서:
		// 1. exception.getCause()를 확인 -> BusinessException 발견
		// 2. BusinessException에서 ErrorCode의 메시지 추출
		// 3. 상세 에러 메시지를 URL 인코딩
		// 4. /login 페이지로 리다이렉트 (상세 에러 정보 포함)
		oAuthFailureHandler.onAuthenticationFailure(request, response, exception);

		// ===== Then: BusinessException의 상세 메시지로 리다이렉트되었는지 검증 =====
		String detailedErrorMessage = ErrorCode.EMAIL_ALREADY_REGISTERED.getMessage();
		String expectedRedirectUrl = "/login?error=oauth_failed&message="
			+ URLEncoder.encode(detailedErrorMessage, StandardCharsets.UTF_8);

		verify(response, times(1)).sendRedirect(expectedRedirectUrl);
	}
}
