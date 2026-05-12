package com.gathering.auth.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.gathering.user.domain.model.UsersEntity;

import lombok.Getter;

@Getter
public class OAuthPrincipal implements UserDetails, OAuth2User {

	private final UsersEntity user;
	private final Map<String, Object> attributes;
	private final boolean linkMode;

	/**
	 * 기본 생성자 (로그인/가입 모드)
	 */
	public OAuthPrincipal(UsersEntity user, Map<String, Object> attributes) {
		this(user, attributes, false);
	}

	/**
	 * 연동 모드 생성자
	 */
	public OAuthPrincipal(UsersEntity user, Map<String, Object> attributes, boolean linkMode) {
		this.user = user;
		this.attributes = attributes;
		this.linkMode = linkMode;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return user.getTsid();
	}

	@Override
	public String getName() {
		return user.getEmail();
	}
}
