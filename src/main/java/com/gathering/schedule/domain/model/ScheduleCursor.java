package com.gathering.schedule.domain.model;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 일정 목록 커서 Value Object
 * 목록이 시작 시각으로 정렬되므로 시작 시각만으로는 같은 시각의 일정을 구분할 수 없어 TSID를 함께 담는다
 * 인코딩 형식: {startAt ISO-8601}_{tsid}
 */
@Getter
public class ScheduleCursor {

	private static final String DELIMITER = "_";

	private final Instant startAt;
	private final String tsid;

	public ScheduleCursor(Instant startAt, String tsid) {
		this.startAt = startAt;
		this.tsid = tsid;
	}

	/**
	 * 마지막으로 조회된 일정으로부터 다음 페이지 커서 생성
	 */
	public static ScheduleCursor from(ScheduleEntity schedule) {
		return new ScheduleCursor(schedule.getStartAt(), schedule.getTsid());
	}

	/**
	 * 클라이언트가 전달한 커서 문자열 해석
	 *
	 * @param encoded 인코딩된 커서 (null이면 첫 페이지)
	 * @return 해석된 커서, 입력이 null이면 null
	 * @throws BusinessException 형식이 올바르지 않은 경우
	 */
	public static ScheduleCursor decode(String encoded) {
		if (encoded == null) {
			return null;
		}

		int delimiterIndex = encoded.lastIndexOf(DELIMITER);
		if (delimiterIndex <= 0 || delimiterIndex == encoded.length() - 1) {
			throw new BusinessException(ErrorCode.INVALID_CURSOR);
		}

		try {
			Instant startAt = Instant.parse(encoded.substring(0, delimiterIndex));
			String tsid = encoded.substring(delimiterIndex + 1);
			return new ScheduleCursor(startAt, tsid);
		} catch (DateTimeParseException e) {
			throw new BusinessException(ErrorCode.INVALID_CURSOR);
		}
	}

	public String encode() {
		return startAt.toString() + DELIMITER + tsid;
	}
}
