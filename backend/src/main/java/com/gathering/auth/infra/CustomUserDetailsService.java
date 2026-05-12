package com.gathering.auth.infra;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.user.domain.model.UserSecurityEntity;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UserSecurityRepository;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security UserDetailsService 구현체
 * 로그인 시 사용자 정보를 조회하여 인증 처리
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UsersRepository usersRepository;
	private final UserSecurityRepository userSecurityRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// 1. 사용자 기본 정보 조회
		UsersEntity user = usersRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

		// 2. 사용자 보안 정보 조회 (비밀번호 해시)
		UserSecurityEntity userSecurity = userSecurityRepository.findByUserTsid(user.getTsid())
			.orElseThrow(() -> new UsernameNotFoundException("보안 정보를 찾을 수 없습니다: " + email));

		// 3. UserDetails 객체 생성
		return User.builder()
			.username(user.getEmail())
			.password(Optional.ofNullable(userSecurity.getPasswordHash()).orElse("oauth2_user"))
			.authorities(getAuthorities())
			.build();
	}

	/**
	 * 사용자 권한 목록 반환
	 * 현재는 기본 권한만 부여, 추후 권한 시스템 구현 시 확장
	 */
	private Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
	}
}
