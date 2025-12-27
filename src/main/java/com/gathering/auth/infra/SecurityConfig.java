package com.gathering.auth.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gathering.auth.application.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PERMIT_ALL_URLS = {
		"/login", "/signup", "/refresh", "/logout", "/", "/users/join", "/error", "/favicon.ico",
		// OAuth 엔드포인트 (인증 불필요)
		"/oauth/**", "/login/oauth2/**",
		// API 문서
		"/docs/**", "/redoc.html", "/my-info"
	};

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuthSuccessHandler oAuthSuccessHandler;
	private final OAuthFailureHandler oAuthFailureHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		configureAuthorization(http);
		configureSessionManagement(http);
		configureSecurityFeatures(http);
		configureExceptionHandling(http);
		configureJwtFilter(http);
		configureOAuth2(http);

		return http.build();
	}

	/**
	 * AuthenticationManager 빈 등록
	 * 로그인 처리 시 인증을 담당하는 핵심 컴포넌트
	 * AuthenticationConfiguration이 자동으로 다음을 수행합니다:
	 * - UserDetailsService 를 구현한 빈을 찾아서 DaoAuthenticationProvider에 설정
	 * - PasswordEncoder 빈을 찾아서 DaoAuthenticationProvider에 설정
	 * - 구성된 DaoAuthenticationProvider를 AuthenticationManager에 등록
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
		throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	/**
	 * 인증/인가 설정
	 */
	private void configureAuthorization(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
			.requestMatchers(PERMIT_ALL_URLS).permitAll()
			.anyRequest().authenticated()
		);
	}

	/**
	 * 세션 관리 설정
	 * JWT 기반 인증을 사용하므로 STATELESS 정책 적용
	 */
	private void configureSessionManagement(HttpSecurity http) throws Exception {
		http.sessionManagement(sessionManagement ->
			sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		);
	}

	/**
	 * 보안 기능 설정
	 * JWT 기반 인증을 사용하므로 기본 인증 방식들을 비활성화
	 */
	private void configureSecurityFeatures(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		http.logout(AbstractHttpConfigurer::disable);
	}

	/**
	 * 예외 처리 설정
	 * 인증/인가 실패 시 커스텀 핸들러 사용
	 */
	private void configureExceptionHandling(HttpSecurity http) throws Exception {
		http.exceptionHandling(exceptionHandling -> exceptionHandling
			.authenticationEntryPoint(jwtAuthenticationEntryPoint)
			.accessDeniedHandler(jwtAccessDeniedHandler)
		);
	}

	/**
	 * JWT 인증 필터 등록
	 * UsernamePasswordAuthenticationFilter 전에 배치하는 이유:
	 * 1. Spring Security의 표준 패턴 (공식 문서 권장 방식)
	 * 2. 커스텀 인증 필터는 관례적으로 UsernamePasswordAuthenticationFilter 전에 배치
	 * 3. JWT 필터가 먼저 실행되어 토큰을 검증하고 SecurityContext에 인증 정보 설정
	 * 4. formLogin을 disabled 했으므로 UsernamePasswordAuthenticationFilter는 실제로 동작하지 않음
	 *    (단지 필터 체인에서의 "위치 참조점"으로 사용)
	 */
	private void configureJwtFilter(HttpSecurity http) {
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
	}

	/**
	 * OAuth2 로그인 설정
	 * @param http HttpSecurity 객체
	 */
	private void configureOAuth2(HttpSecurity http) throws Exception {
		http.oauth2Login(configurer ->
			configurer
				.userInfoEndpoint(endpoint -> endpoint.userService(customOAuth2UserService))
				.successHandler(oAuthSuccessHandler)
				.failureHandler(oAuthFailureHandler)
		);
	}
}
