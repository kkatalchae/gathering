package com.gathering.auth.infra;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		JwtAccessDeniedHandler jwtAccessDeniedHandler) {
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
	}

	private static final String[] PERMIT_ALL_URLS = {
		"/login", "/signup", "/", "/users/join", "/error", "/favicon.ico",
		// API 문서
		"/docs/**", "/redoc.html"
	};

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// 인증 인가
		http.authorizeHttpRequests(authorize -> authorize.requestMatchers(PERMIT_ALL_URLS).permitAll());
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

		// 세션
		http.sessionManagement(
			sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// CSRF
		http.csrf(AbstractHttpConfigurer::disable);

		// 로그인, 로그아웃
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		http.logout(AbstractHttpConfigurer::disable);

		// 예외 처리 (Spring Security 표준 방식)
		http.exceptionHandling(exceptionHandling -> exceptionHandling
			.authenticationEntryPoint(jwtAuthenticationEntryPoint)
			.accessDeniedHandler(jwtAccessDeniedHandler)
		);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		String idForEncode = "bcrypt"; // 신규 가입/변경 시 사용할 기본 알고리즘
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("bcrypt", new BCryptPasswordEncoder());

		return new DelegatingPasswordEncoder(idForEncode, encoders);
	}

	/**
	 * AuthenticationManager 빈 등록
	 * 로그인 처리 시 인증을 담당하는 핵심 컴포넌트
	 * AuthenticationConfiguration이 자동으로 다음을 수행합니다:
	 * - UserDetailsService 를 구현한 빈을 찾아서 DaoAuthenticationProvider에 설정
	 * - PasswordEncoder 빈을 찾아서 DaoAuthenticationProvider에 설정
	 * - 구성된 DaoAuthenticationProvider를 AuthenticationManager에 등록
	 * </ul>
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
		throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}
