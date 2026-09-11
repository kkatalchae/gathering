package com.gathering.schedule.presentation.dto;

import java.time.Instant;

/**
 * 일정 생성/수정 요청이 공통으로 담는 일정 값
 * 서비스가 Value Object 검증을 한 곳에서 처리할 수 있도록 두 요청 DTO를 같은 타입으로 다룬다
 */
public interface ScheduleValuesRequest {

	String getTitle();

	String getDescription();

	Instant getStartAt();

	Instant getEndAt();

	String getLocationName();

	String getLocationAddress();

	Integer getMaxParticipants();
}
