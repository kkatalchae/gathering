package com.gathering.chat.domain.model;

import java.time.Instant;

import com.github.f4b6a3.tsid.Tsid;

/**
 * 메시지 TSID 규칙 — 문자열 비교가 시간순과 일치한다는 전제(docs/adr/0002)에 기대는 계산을 한 곳에 가둔다
 */
public final class ChatMessageTsid {

	/** tsid-creator 규격: 하위 22비트는 난수/카운터, 상위 42비트가 TSID_EPOCH(2020-01-01) 기준 밀리초 */
	private static final int RANDOM_BITS = 22;

	private ChatMessageTsid() {
	}

	/**
	 * 형식이 올바른 메시지 TSID 인지 (13자 Crockford base32)
	 */
	public static boolean isValid(String messageTsid) {
		return messageTsid != null && Tsid.isValid(messageTsid);
	}

	/**
	 * 주어진 시각에 만들어질 수 있는 가장 작은 TSID
	 * 그 시각 이후에 생성된 모든 메시지 TSID 보다 문자열 순서가 앞선다 — 읽음 위치가 없는 사용자의 "참여 이후" 커서로 쓴다
	 *
	 * @param instant 기준 시각 (TSID_EPOCH 이전이면 epoch 로 본다)
	 */
	public static String floorOf(Instant instant) {
		long millisSinceEpoch = Math.max(0, instant.toEpochMilli() - Tsid.TSID_EPOCH);
		return Tsid.from(millisSinceEpoch << RANDOM_BITS).toString();
	}

	/**
	 * 둘 중 더 나중 TSID (null 은 없는 것으로 본다)
	 */
	public static String later(String a, String b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return a.compareTo(b) >= 0 ? a : b;
	}
}
