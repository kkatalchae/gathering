package com.gathering.chat.application;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatMessageContent;
import com.gathering.chat.domain.model.ChatMessageEntity;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.model.ChatRoomReadPositionEntity;
import com.gathering.chat.domain.policy.ChatRoomPolicy;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.chat.presentation.dto.ChatMessageListResponse;
import com.gathering.chat.presentation.dto.ChatMessageResponse;
import com.gathering.chat.presentation.dto.ChatReadPositionResponse;
import com.gathering.chat.presentation.dto.ChatSenderSummary;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/**
 * 채팅 메시지 Application Service
 * 쓰기 순서는 항상 "MySQL 검증(읽기) → Cassandra 쓰기" 다. MySQL 쓰기가 없으므로 같은 메서드에서 해도 롤백될 것이 없다 (docs/adr/0002)
 * 메시지 조회는 파티션 키 (room, 월 버킷) 를 반드시 지정하며, 페이지가 버킷 경계를 넘으면 이전/다음 달 버킷으로 이어서 읽는다
 */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomReadPositionRepository readPositionRepository;
	private final UsersRepository usersRepository;
	private final ChatRoomPolicy chatRoomPolicy;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 메시지 전송
	 * 저장 후 ChatMessageSentEvent 를 발행한다 — 실시간 전달은 이벤트 리스너(STOMP 브로드캐스터)의 몫
	 *
	 * @return 저장된 메시지 (발신자 정보 포함)
	 */
	@Transactional(readOnly = true)
	public ChatMessageResponse sendMessage(String roomTsid, String senderTsid, String content) {
		ChatRoomEntity room = getMemberRoom(roomTsid, senderTsid);
		ChatMessageContent validated = new ChatMessageContent(content);

		ChatMessageEntity saved = chatMessageRepository.save(
			ChatMessageEntity.text(room.getTsid(), senderTsid, validated.getValue()));

		UsersEntity sender = usersRepository.findById(senderTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		ChatMessageResponse response = ChatMessageResponse.from(saved, ChatSenderSummary.from(sender));

		eventPublisher.publishEvent(new ChatMessageSentEvent(room.getTsid(), response));
		return response;
	}

	/**
	 * 메시지 목록 조회 (이전 페이지, 최신순)
	 * before 가 null 이면 가장 최근부터. 현재 버킷에서 size 를 못 채우면 이전 달 버킷으로 내려가며,
	 * 하한은 방 생성 월이다 (그보다 오래된 메시지는 존재할 수 없다)
	 *
	 * @param before 이 메시지보다 오래된 것부터 (null 이면 첫 페이지)
	 */
	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessagesBefore(String roomTsid, String userTsid, String before, int size) {
		ChatRoomEntity room = getMemberRoom(roomTsid, userTsid);
		ChatMessageBucket oldest = ChatMessageBucket.of(room.getCreatedAt());
		ChatMessageBucket bucket = before == null ? ChatMessageBucket.of(Instant.now()) : bucketOf(before);

		List<ChatMessageEntity> collected = new ArrayList<>();
		String cursor = before;
		while (collected.size() <= size && !bucket.isBefore(oldest)) {
			Limit limit = Limit.of(size + 1 - collected.size());
			collected.addAll(cursor == null
				? chatMessageRepository.findByKeyRoomTsidAndKeyBucket(roomTsid, bucket.getValue(), limit)
				: chatMessageRepository.findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidLessThan(
					roomTsid, bucket.getValue(), cursor, limit));
			// 이전 달 버킷부터는 커서 조건이 필요 없다 — 그 달의 모든 메시지가 커서보다 오래됐다
			cursor = null;
			bucket = bucket.previous();
		}

		return toPage(collected, size);
	}

	/**
	 * 놓친 메시지 보충 (after 이후, 오래된 순)
	 * 소켓이 끊겼다 재접속한 클라이언트가 마지막으로 받은 메시지 이후를 채운다 (docs/adr/0003)
	 * after 의 버킷부터 현재 버킷까지 올라간다
	 */
	@Transactional(readOnly = true)
	public ChatMessageListResponse getMessagesAfter(String roomTsid, String userTsid, String after, int size) {
		getMemberRoom(roomTsid, userTsid);
		ChatMessageBucket current = ChatMessageBucket.of(Instant.now());
		ChatMessageBucket bucket = bucketOf(after);

		List<ChatMessageEntity> collected = new ArrayList<>();
		String cursor = after;
		while (collected.size() <= size && !current.isBefore(bucket)) {
			Limit limit = Limit.of(size + 1 - collected.size());
			collected.addAll(cursor == null
				? chatMessageRepository.findByKeyRoomTsidAndKeyBucketOrderByKeyMessageTsidAsc(
					roomTsid, bucket.getValue(), limit)
				: chatMessageRepository.findByKeyRoomTsidAndKeyBucketAndKeyMessageTsidGreaterThanOrderByKeyMessageTsidAsc(
					roomTsid, bucket.getValue(), cursor, limit));
			cursor = null;
			bucket = bucket.next();
		}

		return toPage(collected, size);
	}

	/**
	 * 읽음 위치 갱신 — 앞으로만 옮겨진다
	 */
	@Transactional(readOnly = true)
	public ChatReadPositionResponse markAsRead(String roomTsid, String userTsid, String lastReadMessageTsid) {
		getMemberRoom(roomTsid, userTsid);

		ChatRoomReadPositionEntity position = readPositionRepository
			.findByKeyUserTsidAndKeyRoomTsid(userTsid, roomTsid)
			.orElse(null);
		if (position == null) {
			position = readPositionRepository.save(
				ChatRoomReadPositionEntity.of(userTsid, roomTsid, lastReadMessageTsid));
		} else if (position.advanceTo(lastReadMessageTsid)) {
			// Cassandra 엔티티는 변경 감지가 없다 — 옮겨졌을 때만 명시적으로 저장
			position = readPositionRepository.save(position);
		}

		return ChatReadPositionResponse.from(position);
	}

	private ChatRoomEntity getMemberRoom(String roomTsid, String userTsid) {
		ChatRoomEntity room = chatRoomRepository.findById(roomTsid)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
		chatRoomPolicy.validateMember(room, userTsid);
		return room;
	}

	private ChatMessageBucket bucketOf(String messageTsid) {
		try {
			return ChatMessageBucket.ofMessageTsid(messageTsid);
		} catch (IllegalArgumentException | DateTimeParseException e) {
			throw new BusinessException(ErrorCode.INVALID_CURSOR);
		}
	}

	/**
	 * size+1 로 모은 결과에서 hasNext 를 판단하고 발신자 정보를 IN 한 번으로 붙인다
	 */
	private ChatMessageListResponse toPage(List<ChatMessageEntity> collected, int size) {
		boolean hasNext = collected.size() > size;
		List<ChatMessageEntity> page = hasNext ? collected.subList(0, size) : collected;

		Map<String, ChatSenderSummary> senders = findSenders(page);
		List<ChatMessageResponse> messages = page.stream()
			.map(message -> ChatMessageResponse.from(message, senders.get(message.getSenderTsid())))
			.toList();
		String nextCursor = hasNext ? page.getLast().getMessageTsid() : null;

		return ChatMessageListResponse.of(messages, nextCursor, hasNext);
	}

	/**
	 * 발신자 TSID 를 모아 users 를 한 번에 조회 (조인이 없는 Cassandra 의 대안, 메시지별 반복 조회 금지)
	 * SYSTEM 메시지의 sender 는 null 이라 제외한다
	 */
	private Map<String, ChatSenderSummary> findSenders(List<ChatMessageEntity> messages) {
		List<String> senderTsids = messages.stream()
			.map(ChatMessageEntity::getSenderTsid)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		if (senderTsids.isEmpty()) {
			return Map.of();
		}
		return usersRepository.findAllById(senderTsids).stream()
			.collect(Collectors.toMap(UsersEntity::getTsid, ChatSenderSummary::from, (a, b) -> a));
	}
}
