package com.gathering.auth.infra;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	private static final String[] PERMIT_ALL_URLS = {"/login", "/signup", "/", "/users/join", "/error", "/favicon.ico"};

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

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		String idForEncode = "bcrypt"; // 신규 가입/변경 시 사용할 기본 알고리즘
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("bcrypt", new BCryptPasswordEncoder());

		return new DelegatingPasswordEncoder(idForEncode, encoders);
	}
}
