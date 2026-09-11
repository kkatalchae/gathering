package com.gathering.chat.domain.model;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.github.f4b6a3.tsid.Tsid;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 채팅 메시지 파티션 버킷 Value Object
 * 메시지 파티션 키는 (room_tsid, bucket) 이며 bucket 은 메시지 시각의 'yyyy-MM'(UTC) 이다.
 * 방 하나가 아무리 활발해도 파티션이 한 달치로 잘리도록 한다 — docs/adr/0002 "파티션 크기 상한" 참고
 * 버킷 규칙(형식, 시간대, 이전 버킷 계산)은 반드시 이 클래스를 통해서만 다룬다
 */
@Getter
@EqualsAndHashCode
public class ChatMessageBucket {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

	private final YearMonth yearMonth;

	private ChatMessageBucket(YearMonth yearMonth) {
		this.yearMonth = yearMonth;
	}

	public static ChatMessageBucket of(Instant instant) {
		return new ChatMessageBucket(YearMonth.from(instant.atZone(ZoneOffset.UTC)));
	}

	/**
	 * 메시지 TSID 에 담긴 생성 시각으로 버킷을 계산한다 (커서에서 버킷을 복원할 때 사용)
	 */
	public static ChatMessageBucket ofMessageTsid(String messageTsid) {
		return of(Tsid.from(messageTsid).getInstant());
	}

	public static ChatMessageBucket parse(String value) {
		return new ChatMessageBucket(YearMonth.parse(value, FORMAT));
	}

	/** 한 달 전 버킷 — 현재 버킷에서 페이지를 다 못 채웠을 때 이어서 조회할 파티션 */
	public ChatMessageBucket previous() {
		return new ChatMessageBucket(yearMonth.minusMonths(1));
	}

	public boolean isBefore(ChatMessageBucket other) {
		return yearMonth.isBefore(other.yearMonth);
	}

	/** 파티션 키 컬럼에 저장되는 문자열 */
	public String getValue() {
		return yearMonth.format(FORMAT);
	}
}
