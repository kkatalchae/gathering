package com.gathering.schedule.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ScheduleCursor Value Object 테스트
 */
class ScheduleCursorTest {

	private static final Instant START_AT = Instant.parse("2026-09-20T10:00:00.123456Z");
	private static final String TSID = "01HQSCHEDULE01";

	@Test
	@DisplayName("인코딩한 커서를 다시 디코딩하면 같은 값이 나온다")
	void encodeAndDecodeRoundTrip() {
		// given
		ScheduleCursor cursor = new ScheduleCursor(START_AT, TSID);

		// when
		ScheduleCursor decoded = ScheduleCursor.decode(cursor.encode());

		// then
		assertThat(decoded.getStartAt()).isEqualTo(START_AT);
		assertThat(decoded.getTsid()).isEqualTo(TSID);
	}

	@Test
	@DisplayName("마이크로초 단위 시각도 정밀도 손실 없이 커서에 담긴다")
	void encodePreservesSubSecondPrecision() {
		// given
		ScheduleCursor cursor = new ScheduleCursor(START_AT, TSID);

		// when & then
		assertThat(cursor.encode()).isEqualTo("2026-09-20T10:00:00.123456Z_" + TSID);
	}

	@Test
	@DisplayName("일정 엔티티로부터 다음 페이지 커서를 만들 수 있다")
	void fromSchedule() {
		// given
		ScheduleEntity schedule = ScheduleEntity.builder()
			.tsid(TSID)
			.title("9월 정기 모임")
			.startAt(START_AT)
			.locationName("한강공원 2주차장")
			.createdBy("01HQUSERHOST01")
			.build();

		// when
		ScheduleCursor cursor = ScheduleCursor.from(schedule);

		// then
		assertThat(cursor.getStartAt()).isEqualTo(START_AT);
		assertThat(cursor.getTsid()).isEqualTo(TSID);
	}

	@Test
	@DisplayName("커서가 null이면 첫 페이지를 뜻하므로 null을 반환한다")
	void decodeNull() {
		// when & then
		assertThat(ScheduleCursor.decode(null)).isNull();
	}

	@Test
	@DisplayName("구분자가 없는 커서는 예외가 발생한다")
	void decodeWithoutDelimiter() {
		// when & then
		assertThatThrownBy(() -> ScheduleCursor.decode("2026-09-20T10:00:00Z"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURSOR);
	}

	@Test
	@DisplayName("시각 부분이 ISO-8601 형식이 아니면 예외가 발생한다")
	void decodeWithInvalidInstant() {
		// when & then
		assertThatThrownBy(() -> ScheduleCursor.decode("not-a-time_" + TSID))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURSOR);
	}

	@Test
	@DisplayName("TSID 부분이 비어 있으면 예외가 발생한다")
	void decodeWithEmptyTsid() {
		// when & then
		assertThatThrownBy(() -> ScheduleCursor.decode("2026-09-20T10:00:00Z_"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURSOR);
	}
}
