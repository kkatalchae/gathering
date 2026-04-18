package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.ParticipantRole;

import lombok.Builder;
import lombok.Getter;

/**
 * 참여자 역할 변경 응답 DTO
 */
@Getter
@Builder
public class ChangeParticipantRoleResponse {

	private String participantTsid;
	private String userTsid;
	private ParticipantRole previousRole;
	private ParticipantRole newRole;

	public static ChangeParticipantRoleResponse of(
		String participantTsid,
		String userTsid,
		ParticipantRole previousRole,
		ParticipantRole newRole) {
		return ChangeParticipantRoleResponse.builder()
			.participantTsid(participantTsid)
			.userTsid(userTsid)
			.previousRole(previousRole)
			.newRole(newRole)
			.build();
	}
}
