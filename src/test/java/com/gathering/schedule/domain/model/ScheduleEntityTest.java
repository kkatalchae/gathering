package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ScheduleEntity 도메인 로직 테스트
 */
class ScheduleEntityTest {

	@Test
	@DisplayName("모임 TSID가 있으면 모임에 귀속된 일정으로 판단한다")
	void isAttachedToGatheringWhenGatheringTsidExists() {
		// given
		ScheduleEntity schedule = schedule("01HQGATHERING1");

		// when & then
		assertThat(schedule.isAttachedToGathering()).isTrue();
	}

	@Test
	@DisplayName("모임 TSID가 없으면 일회성 독립 일정으로 판단한다")
	void isNotAttachedToGatheringWhenGatheringTsidIsNull() {
		// given
		ScheduleEntity schedule = schedule(null);

		// when & then
		assertThat(schedule.isAttachedToGathering()).isFalse();
	}

	private ScheduleEntity schedule(String gatheringTsid) {
		return ScheduleEntity.builder()
			.tsid("01HQSCHEDULE01")
			.gatheringTsid(gatheringTsid)
			.title("9월 정기 모임")
			.startAt(Instant.parse("2026-09-20T10:00:00Z"))
			.locationName("한강공원 2주차장")
			.createdBy("01HQUSER000001")
			.build();
	}
}
