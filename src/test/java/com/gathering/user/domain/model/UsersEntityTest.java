package com.gathering.user.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UsersEntity 도메인 로직 테스트
 */
class UsersEntityTest {

	@Test
	@DisplayName("탈퇴하면 개인정보가 모두 지워지고 상태가 WITHDRAWN 이 된다")
	void withdrawAnonymizes() {
		// given
		UsersEntity user = UsersEntity.builder()
			.tsid("01HQUSER000001")
			.email("kim@example.com")
			.name("김병채")
			.nickname("러닝맨")
			.phoneNumber("010-1234-5678")
			.profileImageUrl("https://example.com/me.jpg")
			.emailVerified(true)
			.build();

		// when
		user.withdraw();

		// then
		assertThat(user.isWithdrawn()).isTrue();
		assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(user.getEmail()).isEqualTo("withdrawn+01HQUSER000001@withdrawn.invalid");
		assertThat(user.getName()).isEqualTo(UsersEntity.WITHDRAWN_NAME);
		assertThat(user.getNickname()).isNull();
		assertThat(user.getPhoneNumber()).isNull();
		assertThat(user.getProfileImageUrl()).isNull();
		assertThat(user.getEmailVerified()).isFalse();
		assertThat(user.getWithdrawnAt()).isNotNull();
		// 참조 무결성을 위해 식별자는 그대로다
		assertThat(user.getTsid()).isEqualTo("01HQUSER000001");
	}

	@Test
	@DisplayName("탈퇴한 사용자의 익명 이메일은 사용자마다 달라 uk_user_email 과 충돌하지 않는다")
	void withdrawnEmailIsUniquePerUser() {
		// given
		UsersEntity first = UsersEntity.builder().tsid("01HQUSER000001").email("a@example.com").name("a").build();
		UsersEntity second = UsersEntity.builder().tsid("01HQUSER000002").email("b@example.com").name("b").build();

		// when
		first.withdraw();
		second.withdraw();

		// then
		assertThat(first.getEmail()).isNotEqualTo(second.getEmail());
	}
}
