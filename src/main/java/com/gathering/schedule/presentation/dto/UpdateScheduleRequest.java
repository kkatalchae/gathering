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
 * 일정 수정 요청 DTO
 * 귀속 모임은 변경할 수 없다
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateScheduleRequest implements ScheduleValuesRequest {

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
