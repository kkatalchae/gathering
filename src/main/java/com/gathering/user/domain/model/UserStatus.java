package com.gathering.user.domain.model;

public enum UserStatus {
	ACTIVE,
	/** 탈퇴 — 개인정보는 익명화됐고 row 는 참조 무결성을 위해 남아 있다 */
	WITHDRAWN
}
