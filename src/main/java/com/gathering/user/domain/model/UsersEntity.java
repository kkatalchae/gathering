package com.gathering.user.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_email", columnNames = "email")
	}
)
public class UsersEntity {

	@Id
	@Tsid
	@Column(nullable = false, length = 13, columnDefinition = "CHAR(13)")
	private String tsid;

	@Column(nullable = false, length = 320)
	private String email;

	@Column
	private String nickname;

	@Column(nullable = false)
	private String name;

	// TODO 추후 문자를 통해 검증
	@Column
	private String phoneNumber;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Builder.Default
	@Column(name = "email_verified", nullable = false)
	private Boolean emailVerified = false;

	@Builder.Default
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Instant createdAt;

	/** 탈퇴 시각. 익명화된 row 를 나중에 완전 파기하는 배치의 기준이 된다 */
	@Column(name = "withdrawn_at")
	private Instant withdrawnAt;


	/** 익명화된 이메일 도메인. RFC 2606 예약 TLD 라 실제 주소와 절대 겹치지 않고, uk_user_email 도 tsid 로 유일하다 */
	public static final String WITHDRAWN_EMAIL_DOMAIN = "withdrawn.invalid";
	public static final String WITHDRAWN_NAME = "탈퇴한 사용자";

	/**
	 * 회원 탈퇴 — 개인정보를 지우고 상태를 WITHDRAWN 으로 바꾼다
	 * row 자체는 남긴다: 일정 호스트, 채팅 발신자처럼 이 사용자를 가리키는 기록이 "탈퇴한 사용자" 로 계속 유효해야 하고,
	 * FK 를 하나씩 끊는 대신 참조 무결성을 그대로 두는 편이 안전하다. 원래 이메일은 비워지므로 같은 이메일로 재가입할 수 있다
	 */
	public void withdraw() {
		this.email = "withdrawn+" + tsid + "@" + WITHDRAWN_EMAIL_DOMAIN;
		this.name = WITHDRAWN_NAME;
		this.nickname = null;
		this.phoneNumber = null;
		this.profileImageUrl = null;
		this.emailVerified = false;
		this.status = UserStatus.WITHDRAWN;
		this.withdrawnAt = Instant.now();
	}

	public boolean isWithdrawn() {
		return status == UserStatus.WITHDRAWN;
	}

	public void updateProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	/**
	 * 프로필 정보 업데이트
	 * JPA 엔티티이므로 필드를 직접 변경하면 dirty checking으로 자동 UPDATE
	 *
	 * @param nickname 닉네임 (null 또는 빈 문자열이면 변경하지 않음)
	 * @param name 이름 (null 또는 빈 문자열이면 변경하지 않음)
	 * @param phoneNumber 전화번호 (null 또는 빈 문자열이면 변경하지 않음)
	 */
	public void updateProfile(String nickname, String name, String phoneNumber) {
		if (nickname != null && !nickname.isBlank()) {
			this.nickname = nickname;
		}
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		if (phoneNumber != null && !phoneNumber.isBlank()) {
			this.phoneNumber = phoneNumber;
		}
	}
}
