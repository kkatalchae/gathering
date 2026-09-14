package com.gathering.chat.infra;

import java.security.Principal;

/**
 * STOMP 세션에 붙는 사용자 식별자 — CONNECT 에서 검증한 JWT 의 TSID
 * 이후 프레임(SUBSCRIBE 등)에서 accessor.getUser() 로 꺼내 쓴다
 */
public record StompUserPrincipal(String userTsid) implements Principal {

	@Override
	public String getName() {
		return userTsid;
	}
}
