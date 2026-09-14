package com.gathering.chat.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatMessageTsid;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.model.ChatRoomMembership;
import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.chat.presentation.dto.ChatMessageResponse;
import com.gathering.chat.presentation.dto.ChatRoomListResponse;
import com.gathering.chat.presentation.dto.ChatRoomSummaryResponse;
import com.gathering.chat.presentation.dto.ChatSenderSummary;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.schedule.domain.model.ScheduleParticipantEntity;
import com.gathering.schedule.domain.repository.ScheduleParticipantRepository;

import lombok.RequiredArgsConstructor;

/**
 * 내 채팅방 목록 — 멤버십은 참여자 테이블에서 파생하고(docs/adr/0004), 마지막 메시지·안 읽은 수는 Cassandra 에서 방마다 읽는다
 * 비용: MySQL 4회(모임/일정 참여, 방 2종) + Cassandra 1회(읽음 위치) + 방마다 최소 2회(마지막 메시지, 안 읽은 키).
 * 버킷이 비어 있으면 이전 달로 내려가므로 오래되고 조용한 방일수록 조회가 늘어난다 — 목록이 병목이 되면 ADR-0002 의 전환 기준을 따른다
 */
@Service
@RequiredArgsConstructor
public class ChatRoomListService {

	/** 안 읽은 키를 이만큼까지만 읽는다 — 표시 상한(99)을 넘었는지만 알면 된다 */
	static final int UNREAD_FETCH_LIMIT = ChatRoomSummaryResponse.UNREAD_DISPLAY_MAX + 1;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomReadPositionRepository readPositionRepository;
	private final GatheringParticipantRepository gatheringParticipantRepository;
	private final ScheduleParticipantRepository scheduleParticipantRepository;
	private final ChatSenderResolver senderResolver;

	/**
	 * 사용자가 속한 모든 채팅방 (참여한 모임의 방 ∪ 참여한 일정의 방)
	 * 정렬: 마지막 메시지가 최근인 방부터, 메시지가 없는 방은 뒤에 생성 역순
	 */
	@Transactional(readOnly = true)
	public ChatRoomListResponse getMyRooms(String userTsid) {
		List<ChatRoomMembership> memberships = findMemberships(userTsid);
		Map<String, String> readPositions = readPositionRepository.findByKeyUserTsid(userTsid).stream()
			.collect(Collectors.toMap(ChatRoomReadPositionEntity::getRoomTsid,
				ChatRoomReadPositionEntity::getLastReadMessageTsid));
		ChatMessageBucket current = ChatMessageBucket.of(Instant.now());

		List<RoomState> states = new ArrayList<>();
		for (ChatRoomMembership membership : memberships) {
			ChatRoomEntity room = membership.room();
			String lastRead = readPositions.get(room.getTsid());
			// 읽음 위치가 없으면 주체에 참여한 시각 이후가 "안 읽은" 이다. 탈퇴 후 재참여처럼 둘 다 있으면 더 나중 쪽
			String unreadCursor = ChatMessageTsid.later(lastRead, ChatMessageTsid.floorOf(membership.joinedAt()));
			states.add(new RoomState(membership, lastRead,
				findLastMessage(room, current), countUnread(room, unreadCursor, current)));
		}

		Map<String, ChatSenderSummary> senders = senderResolver.resolve(states.stream()
			.map(RoomState::lastMessage)
			.filter(Objects::nonNull)
			.map(ChatMessageEntity::getSenderTsid)
			.toList());

		List<ChatRoomSummaryResponse> rooms = states.stream()
			.sorted(Comparator
				.comparing((RoomState state) -> state.lastMessageTsid(),
					Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(state -> state.membership().room().getCreatedAt(), Comparator.reverseOrder()))
			.map(state -> state.toResponse(senders))
			.toList();

		return ChatRoomListResponse.of(rooms);
	}

	/**
	 * 모임/일정 참여 row 로 방을 찾는다 — 타입별 2회씩, 방마다 반복 조회하지 않는다
	 */
	private List<ChatRoomMembership> findMemberships(String userTsid) {
		Map<String, Instant> gatheringJoinedAt = gatheringParticipantRepository.findAllByUserTsid(userTsid).stream()
			.collect(Collectors.toMap(GatheringParticipantEntity::getGatheringTsid,
				GatheringParticipantEntity::getJoinedAt));
		Map<String, Instant> scheduleJoinedAt = scheduleParticipantRepository.findAllByUserTsid(userTsid).stream()
			.collect(Collectors.toMap(ScheduleParticipantEntity::getScheduleTsid,
				ScheduleParticipantEntity::getJoinedAt));

		List<ChatRoomMembership> memberships = new ArrayList<>();
		if (!gatheringJoinedAt.isEmpty()) {
			chatRoomRepository.findAllByGatheringTsidInWithGathering(List.copyOf(gatheringJoinedAt.keySet()))
				.forEach(room -> memberships.add(
					new ChatRoomMembership(room, gatheringJoinedAt.get(room.getGatheringTsid()))));
		}
		if (!scheduleJoinedAt.isEmpty()) {
			chatRoomRepository.findAllByScheduleTsidInWithSchedule(List.copyOf(scheduleJoinedAt.keySet()))
				.forEach(room -> memberships.add(
					new ChatRoomMembership(room, scheduleJoinedAt.get(room.getScheduleTsid()))));
		}
		return memberships;
	}

	/**
	 * 방의 마지막 메시지 — 현재 버킷부터 방 생성 월까지 내려가며 처음 나오는 1건
	 */
	private ChatMessageEntity findLastMessage(ChatRoomEntity room, ChatMessageBucket current) {
		ChatMessageBucket oldest = ChatMessageBucket.of(room.getCreatedAt());
		for (ChatMessageBucket bucket = current; !bucket.isBefore(oldest); bucket = bucket.previous()) {
			List<ChatMessageEntity> latest = chatMessageRepository.findByKeyRoomTsidAndKeyBucket(
				room.getTsid(), bucket.getValue(), Limit.of(1));
			if (!latest.isEmpty()) {
				return latest.getFirst();
			}
		}
		return null;
	}

	/**
	 * 커서 이후 메시지 수 (UNREAD_FETCH_LIMIT 에서 끊는다)
	 * 현재 버킷부터 커서 버킷까지 내려간다 — 활발한 방일수록 첫 버킷에서 상한에 닿아 끝난다.
	 * 커서 버킷보다 나중 버킷의 메시지는 모두 커서 이후이므로 같은 GreaterThan 조건을 그대로 써도 된다
	 */
	private int countUnread(ChatRoomEntity room, String cursor, ChatMessageBucket current) {
		ChatMessageBucket lower = ChatMessageBucket.max(
			ChatMessageBucket.ofMessageTsid(cursor), ChatMessageBucket.of(room.getCreatedAt()));
		int count = 0;
		for (ChatMessageBucket bucket = current;
			 !bucket.isBefore(lower) && count < UNREAD_FETCH_LIMIT;
			 bucket = bucket.previous()) {
			count += chatMessageRepository.findKeysByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThan(
				room.getTsid(), bucket.getValue(), cursor, Limit.of(UNREAD_FETCH_LIMIT - count)).size();
		}
		return count;
	}

	/**
	 * 방 하나의 조회 결과 — 정렬과 응답 조립 사이의 중간 형태
	 */
	private record RoomState(ChatRoomMembership membership, String lastReadMessageTsid,
							 ChatMessageEntity lastMessage, int unreadCount) {

		String lastMessageTsid() {
			return lastMessage == null ? null : lastMessage.getMessageTsid();
		}

		ChatRoomSummaryResponse toResponse(Map<String, ChatSenderSummary> senders) {
			ChatRoomEntity room = membership.room();
			boolean capped = unreadCount > ChatRoomSummaryResponse.UNREAD_DISPLAY_MAX;
			return ChatRoomSummaryResponse.builder()
				.roomTsid(room.getTsid())
				.roomType(room.getRoomType())
				.gatheringTsid(room.getGatheringTsid())
				.scheduleTsid(room.getScheduleTsid())
				.title(membership.title())
				.createdAt(room.getCreatedAt())
				.lastMessage(lastMessage == null ? null
					: ChatMessageResponse.from(lastMessage, senders.get(lastMessage.getSenderTsid())))
				.lastReadMessageTsid(lastReadMessageTsid)
				.unreadCount(Math.min(unreadCount, ChatRoomSummaryResponse.UNREAD_DISPLAY_MAX))
				.unreadCountCapped(capped)
				.build();
		}
	}
}
