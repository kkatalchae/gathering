package com.gathering.schedule.presentation.dto;

import java.time.Instant;

import com.gathering.schedule.domain.model.ScheduleParticipantEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 참여자 요약 DTO
 * gatheringMember 는 조회 시점에 귀속 모임의 참여자인지로 판정한다 (독립 일정이면 항상 false)
 */
@Getter
@Builder
public class ScheduleParticipantSummary {

	private String userTsid;

	private String nickname;

	private String name;

	private String profileImageUrl;

	private Instant joinedAt;

	/** 일정을 개설한 호스트 여부 */
	private boolean host;

	/** 귀속 모임의 참여자 여부 (게스트면 false) */
	private boolean gatheringMember;

	public static ScheduleParticipantSummary from(ScheduleParticipantEntity participant, boolean host,
		boolean gatheringMember) {
		return ScheduleParticipantSummary.builder()
			.userTsid(participant.getUserTsid())
			.nickname(participant.getUser().getNickname())
			.name(participant.getUser().getName())
			.profileImageUrl(participant.getUser().getProfileImageUrl())
			.joinedAt(participant.getJoinedAt())
			.host(host)
			.gatheringMember(gatheringMember)
			.build();
	}
}
