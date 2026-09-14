package com.gathering.chat.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.auth.application.AuthService;
import com.gathering.chat.application.ChatMessageService;
import com.gathering.chat.application.ChatRoomListService;
import com.gathering.chat.presentation.dto.ChatMessageListResponse;
import com.gathering.chat.presentation.dto.ChatMessageResponse;
import com.gathering.chat.presentation.dto.ChatReadPositionResponse;
import com.gathering.chat.presentation.dto.ChatRoomListResponse;
import com.gathering.chat.presentation.dto.MarkChatReadRequest;
import com.gathering.chat.presentation.dto.SendChatMessageRequest;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 채팅방 API Controller
 * 모든 엔드포인트는 인증 + 채팅방 멤버십이 필요하다 (모임/일정 GET 과 달리 비로그인 허용 없음)
 * 전송은 REST, 실시간 수신은 STOMP 구독 — docs/adr/0003
 */
@RestController
@RequestMapping("/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomsController {

	private static final int MAX_PAGE_SIZE = 100;

	private final ChatMessageService chatMessageService;
	private final ChatRoomListService chatRoomListService;
	private final AuthService authService;

	/**
	 * 내 채팅방 목록 — 참여한 모임/일정의 방 전부, 마지막 메시지와 안 읽은 수 포함
	 */
	@GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ChatRoomListResponse> getMyRooms(HttpServletRequest request) {
		String userTsid = authService.getCurrentUserTsid(request);
		return ResponseEntity.ok(chatRoomListService.getMyRooms(userTsid));
	}

	/**
	 * 메시지 전송
	 */
	@PostMapping("/{roomTsid}/messages")
	public ResponseEntity<ChatMessageResponse> sendMessage(
		HttpServletRequest request,
		@PathVariable String roomTsid,
		@Valid @RequestBody SendChatMessageRequest sendRequest) {

		String userTsid = authService.getCurrentUserTsid(request);
		ChatMessageResponse response = chatMessageService.sendMessage(roomTsid, userTsid, sendRequest.getContent());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 메시지 목록 조회
	 *
	 * @param before 이 메시지보다 오래된 것부터 최신순 (없으면 첫 페이지)
	 * @param after 이 메시지보다 새로운 것부터 오래된 순 (재접속 보충). before 와 함께 쓸 수 없다
	 * @param size 페이지 크기 (기본 50, 최대 100)
	 */
	@GetMapping(value = "/{roomTsid}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ChatMessageListResponse> getMessages(
		HttpServletRequest request,
		@PathVariable String roomTsid,
		@RequestParam(required = false) String before,
		@RequestParam(required = false) String after,
		@RequestParam(defaultValue = "50") int size) {

		if (size <= 0 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
		}
		if (before != null && after != null) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_CURSOR_AMBIGUOUS);
		}

		String userTsid = authService.getCurrentUserTsid(request);
		ChatMessageListResponse response = after != null
			? chatMessageService.getMessagesAfter(roomTsid, userTsid, after, size)
			: chatMessageService.getMessagesBefore(roomTsid, userTsid, before, size);

		return ResponseEntity.ok(response);
	}

	/**
	 * 읽음 위치 갱신
	 */
	@PutMapping("/{roomTsid}/read-position")
	public ResponseEntity<ChatReadPositionResponse> markAsRead(
		HttpServletRequest request,
		@PathVariable String roomTsid,
		@Valid @RequestBody MarkChatReadRequest markRequest) {

		String userTsid = authService.getCurrentUserTsid(request);
		ChatReadPositionResponse response = chatMessageService.markAsRead(
			roomTsid, userTsid, markRequest.getLastReadMessageTsid());

		return ResponseEntity.ok(response);
	}
}
