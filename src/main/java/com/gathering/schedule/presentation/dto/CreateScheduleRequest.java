package com.gathering.schedule.presentation.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 생성 요청 DTO
 * gatheringTsid를 지정하면 해당 모임에 귀속된 일정, 지정하지 않으면 일회성 독립 일정이 된다
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateScheduleRequest implements ScheduleValuesRequest {

	private String gatheringTsid;

	@NotBlank(message = "일정 제목은 필수입니다")
	private String title;

	private String description;

	@NotNull(message = "일정 시작 시각은 필수입니다")
	private Instant startAt;

	private Instant endAt;

	@NotBlank(message = "일정 장소는 필수입니다")
	private String locationName;

	private String locationAddress;

	private Integer maxParticipants;
}
