package com.gathering.chat.infra;

import java.util.Optional;

/**
 * STOMP 목적지 규약 — 채팅방 하나에 토픽 하나
 * 클라이언트는 /topic/rooms/{roomTsid} 를 구독하고, 서버는 메시지 저장이 커밋된 뒤 같은 목적지로 브로드캐스트한다
 */
public final class ChatDestinations {

	public static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

	private ChatDestinations() {
	}

	public static String room(String roomTsid) {
		return ROOM_TOPIC_PREFIX + roomTsid;
	}

	/**
	 * 구독 목적지에서 채팅방 TSID 를 꺼낸다. 규약에 맞지 않는 목적지(다른 토픽, 하위 경로)는 empty
	 */
	public static Optional<String> roomTsidOf(String destination) {
		if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
			return Optional.empty();
		}
		String roomTsid = destination.substring(ROOM_TOPIC_PREFIX.length());
		if (roomTsid.isBlank() || roomTsid.contains("/")) {
			return Optional.empty();
		}
		return Optional.of(roomTsid);
	}
}
