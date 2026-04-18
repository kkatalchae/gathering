package com.gathering.gathering.presentation.dto;

import com.gathering.gathering.domain.model.ParticipantRole;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여자 역할 변경 요청 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeParticipantRoleRequest {

	@NotNull(message = "변경할 역할은 필수입니다")
	private ParticipantRole newRole;
}
