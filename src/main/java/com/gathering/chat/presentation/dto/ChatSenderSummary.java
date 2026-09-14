package com.gathering.chat.presentation.dto;

import com.gathering.user.domain.model.UsersEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 메시지 발신자 요약 DTO
 * 탈퇴한 사용자는 익명화된 이름("탈퇴한 사용자")과 withdrawn=true 로 표시된다 (docs/adr/0002)
 */
@Getter
@Builder
public class ChatSenderSummary {

	private String userTsid;

	private String nickname;

	private String name;

	private String profileImageUrl;

	private boolean withdrawn;

	public static ChatSenderSummary from(UsersEntity user) {
		return ChatSenderSummary.builder()
			.userTsid(user.getTsid())
			.nickname(user.getNickname())
			.name(user.getName())
			.profileImageUrl(user.getProfileImageUrl())
			.withdrawn(user.isWithdrawn())
			.build();
	}
}
