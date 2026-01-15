package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.ParticipantRole;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * 참여자 역할 변경 응답 DTO
 */
@Getter
@Builder
public class ChangeParticipantRoleResponse {

	@NotNull
	private String participantTsid;
	@NotNull
	private String userTsid;
	@NotNull
	private ParticipantRole previousRole;
	@NotNull
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
