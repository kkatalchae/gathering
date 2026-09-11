package com.gathering.schedule.domain.model;

/**
 * 일정별 참여 인원 집계 결과 (JPQL 생성자 표현식으로 조회)
 * 생성자 인자 순서가 쿼리의 SELECT 순서와 일치해야 한다
 */
public record ScheduleParticipantCount(String scheduleTsid, long participantCount) {
}
