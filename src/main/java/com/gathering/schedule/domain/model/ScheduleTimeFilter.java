package com.gathering.schedule.domain.model;

/**
 * 일정 목록 조회 시 기준 시각 대비 어느 쪽 일정을 볼지 결정한다
 */
public enum ScheduleTimeFilter {
	/** 아직 시작하지 않은 일정, 가까운 순 */
	UPCOMING,
	/** 이미 시작한 일정, 최근 순 */
	PAST
}
