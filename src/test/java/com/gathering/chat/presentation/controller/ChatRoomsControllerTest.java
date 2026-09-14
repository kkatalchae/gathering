package com.gathering.chat.presentation.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathering.ApiDocSpec;
import com.gathering.auth.application.AuthService;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.chat.application.ChatMessageService;
import com.gathering.chat.application.ChatRoomListService;
import com.gathering.chat.domain.model.ChatMessageType;
import com.gathering.chat.domain.model.ChatRoomType;
import com.gathering.chat.presentation.dto.ChatMessageListResponse;
import com.gathering.chat.presentation.dto.ChatMessageResponse;
import com.gathering.chat.presentation.dto.ChatReadPositionResponse;
import com.gathering.chat.presentation.dto.ChatRoomListResponse;
import com.gathering.chat.presentation.dto.ChatRoomSummaryResponse;
import com.gathering.chat.presentation.dto.ChatSenderSummary;
import com.gathering.chat.presentation.dto.MarkChatReadRequest;
import com.gathering.chat.presentation.dto.SendChatMessageRequest;
import com.gathering.common.exception.BusinessException;
import com.gathering.common.exception.ErrorCode;

/**
 * ChatRoomsController Spring REST Docs 테스트
 */
@SpringBootTest
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomsControllerTest {

	private static final String USER_TSID = "01HQUSER000001";
	private static final String ROOM_TSID = "01HQROOM000001";
	private static final String MESSAGE_TSID = "0HZZZZZZZZZZ5";

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public ChatMessageService chatMessageService() {
			return Mockito.mock(ChatMessageService.class);
		}

		@Bean
		@Primary
		public ChatRoomListService chatRoomListService() {
			return Mockito.mock(ChatRoomListService.class);
		}

		@Bean
		@Primary
		public AuthService authService() {
			return Mockito.mock(AuthService.class);
		}

		@Bean
		@Primary
		public JwtTokenProvider jwtTokenProvider() {
			return Mockito.mock(JwtTokenProvider.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ChatMessageService chatMessageService;

	@Autowired
	private ChatRoomListService chatRoomListService;

	@Autowired
	private AuthService authService;

	@BeforeEach
	void setUp() {
		Mockito.reset(chatMessageService, chatRoomListService, authService);
		when(authService.getCurrentUserTsid(any())).thenReturn(USER_TSID);
	}

	@Test
	@DisplayName("내 채팅방 목록을 조회하면 200 상태코드와 방마다 마지막 메시지, 안 읽은 수를 반환한다")
	void getMyRoomsSuccess() throws Exception {
		// given
		ChatRoomSummaryResponse gatheringRoom = ChatRoomSummaryResponse.builder()
			.roomTsid(ROOM_TSID).roomType(ChatRoomType.GATHERING).gatheringTsid("01HQGATH000001")
			.title("한강 러닝크루").createdAt(Instant.now())
			.lastMessage(message("이번 주 토요일 어때요?")).lastReadMessageTsid(MESSAGE_TSID)
			.unreadCount(3).unreadCountCapped(false).build();
		ChatRoomSummaryResponse scheduleRoom = ChatRoomSummaryResponse.builder()
			.roomTsid("01HQROOM000002").roomType(ChatRoomType.SCHEDULE).scheduleTsid("01HQSCHD000001")
			.title("9월 정기 모임").createdAt(Instant.now())
			.unreadCount(0).unreadCountCapped(false).build();
		when(chatRoomListService.getMyRooms(USER_TSID))
			.thenReturn(ChatRoomListResponse.of(List.of(gatheringRoom, scheduleRoom)));

		// when & then
		mockMvc.perform(get("/chat-rooms/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rooms.length()").value(2))
			.andExpect(jsonPath("$.rooms[0].unreadCount").value(3))
			.andExpect(jsonPath("$.rooms[1].lastMessage").doesNotExist())
			.andDo(document("chat-rooms-me",
				ApiDocSpec.CHAT_MY_ROOMS.getDescription(),
				ApiDocSpec.CHAT_MY_ROOMS.getSummary(),
				responseFields(
					List.of(
						fieldWithPath("rooms").description("채팅방 목록 (마지막 메시지 최근순, 없으면 생성 역순)"),
						fieldWithPath("rooms[].roomTsid").description("채팅방 TSID"),
						fieldWithPath("rooms[].roomType").description("GATHERING | SCHEDULE"),
						fieldWithPath("rooms[].gatheringTsid").description("모임 TSID (모임 방일 때)").optional(),
						fieldWithPath("rooms[].scheduleTsid").description("일정 TSID (일정 방일 때)").optional(),
						fieldWithPath("rooms[].title").description("모임 이름 또는 일정 제목"),
						fieldWithPath("rooms[].createdAt").description("채팅방 생성 일시"),
						fieldWithPath("rooms[].lastMessage").description("마지막 메시지 (없으면 null)").optional(),
						fieldWithPath("rooms[].lastReadMessageTsid")
							.description("내 읽음 위치 (읽음 처리한 적 없으면 null)").optional(),
						fieldWithPath("rooms[].unreadCount").description("안 읽은 메시지 수 (0~99)"),
						fieldWithPath("rooms[].unreadCountCapped").description("true 면 실제로는 99 보다 많다 (99+)")
					).toArray(new org.springframework.restdocs.payload.FieldDescriptor[0])
				).and(messageFields("rooms[].lastMessage."))
			));
	}

	@Test
	@DisplayName("메시지를 보내면 201 상태코드와 저장된 메시지를 반환한다")
	void sendMessageSuccess() throws Exception {
		// given
		when(chatMessageService.sendMessage(ROOM_TSID, USER_TSID, "이번 주 토요일 어때요?")).thenReturn(message("이번 주 토요일 어때요?"));

		// when & then
		mockMvc.perform(post("/chat-rooms/{roomTsid}/messages", ROOM_TSID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new SendChatMessageRequest("이번 주 토요일 어때요?"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.messageTsid").value(MESSAGE_TSID))
			.andExpect(jsonPath("$.sender.nickname").value("러닝맨"))
			.andDo(document("chat-rooms-send",
				ApiDocSpec.CHAT_SEND_MESSAGE.getDescription(),
				ApiDocSpec.CHAT_SEND_MESSAGE.getSummary(),
				pathParameters(parameterWithName("roomTsid").description("채팅방 TSID")),
				requestFields(fieldWithPath("content").description("메시지 내용 (필수, 1000자 이하)")),
				responseFields(messageFields(""))
			));
	}

	@Test
	@DisplayName("채팅방 멤버가 아니면 403 에러를 반환한다")
	void sendMessageDenied() throws Exception {
		// given
		when(chatMessageService.sendMessage(eq(ROOM_TSID), eq(USER_TSID), any()))
			.thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

		// when & then
		mockMvc.perform(post("/chat-rooms/{roomTsid}/messages", ROOM_TSID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new SendChatMessageRequest("안녕"))))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("내용이 비어 있으면 400 에러를 반환한다")
	void sendMessageBlank() throws Exception {
		mockMvc.perform(post("/chat-rooms/{roomTsid}/messages", ROOM_TSID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new SendChatMessageRequest("  "))))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("메시지 목록을 조회하면 200 상태코드와 최신순 메시지, 커서를 반환한다")
	void getMessagesSuccess() throws Exception {
		// given
		when(chatMessageService.getMessagesBefore(ROOM_TSID, USER_TSID, null, 50))
			.thenReturn(ChatMessageListResponse.of(List.of(message("두 번째"), message("첫 번째")), MESSAGE_TSID, true));

		// when & then
		mockMvc.perform(get("/chat-rooms/{roomTsid}/messages", ROOM_TSID).param("size", "50"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.messages.length()").value(2))
			.andExpect(jsonPath("$.hasNext").value(true))
			.andDo(document("chat-rooms-messages",
				ApiDocSpec.CHAT_GET_MESSAGES.getDescription(),
				ApiDocSpec.CHAT_GET_MESSAGES.getSummary(),
				pathParameters(parameterWithName("roomTsid").description("채팅방 TSID")),
				queryParameters(
					parameterWithName("before").description("이 메시지보다 오래된 것부터 최신순 (선택)").optional(),
					parameterWithName("after").description("이 메시지보다 새로운 것부터 오래된 순, 재접속 보충 (선택)").optional(),
					parameterWithName("size").description("페이지 크기 (기본 50, 최대 100)").optional()
				),
				responseFields(
					List.of(
						fieldWithPath("messages").description("메시지 목록"),
						fieldWithPath("nextCursor").description("다음 페이지 커서 (마지막이면 null)").optional(),
						fieldWithPath("hasNext").description("다음 페이지 존재 여부")
					).toArray(new org.springframework.restdocs.payload.FieldDescriptor[0])
				).and(messageFields("messages[]."))
			));
	}

	@Test
	@DisplayName("after 로 조회하면 놓친 메시지를 오래된 순으로 반환한다")
	void getMessagesAfter() throws Exception {
		// given
		when(chatMessageService.getMessagesAfter(ROOM_TSID, USER_TSID, MESSAGE_TSID, 50))
			.thenReturn(ChatMessageListResponse.of(List.of(message("놓친 메시지")), null, false));

		// when & then
		mockMvc.perform(get("/chat-rooms/{roomTsid}/messages", ROOM_TSID).param("after", MESSAGE_TSID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.messages[0].content").value("놓친 메시지"))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("before 와 after 를 함께 주면 400 에러를 반환한다")
	void getMessagesAmbiguousCursor() throws Exception {
		mockMvc.perform(get("/chat-rooms/{roomTsid}/messages", ROOM_TSID)
				.param("before", MESSAGE_TSID).param("after", MESSAGE_TSID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CHAT_MESSAGE_CURSOR_AMBIGUOUS"));
		verifyNoInteractions(chatMessageService);
	}

	@Test
	@DisplayName("페이지 크기가 범위를 벗어나면 400 에러를 반환한다")
	void getMessagesInvalidSize() throws Exception {
		mockMvc.perform(get("/chat-rooms/{roomTsid}/messages", ROOM_TSID).param("size", "101"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("읽음 위치를 갱신하면 200 상태코드와 현재 읽음 위치를 반환한다")
	void markAsReadSuccess() throws Exception {
		// given
		when(chatMessageService.markAsRead(ROOM_TSID, USER_TSID, MESSAGE_TSID)).thenReturn(
			ChatReadPositionResponse.builder().roomTsid(ROOM_TSID).lastReadMessageTsid(MESSAGE_TSID)
				.updatedAt(Instant.now()).build());

		// when & then
		mockMvc.perform(put("/chat-rooms/{roomTsid}/read-position", ROOM_TSID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new MarkChatReadRequest(MESSAGE_TSID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lastReadMessageTsid").value(MESSAGE_TSID))
			.andDo(document("chat-rooms-read-position",
				ApiDocSpec.CHAT_MARK_READ.getDescription(),
				ApiDocSpec.CHAT_MARK_READ.getSummary(),
				pathParameters(parameterWithName("roomTsid").description("채팅방 TSID")),
				requestFields(fieldWithPath("lastReadMessageTsid").description("마지막으로 읽은 메시지 TSID")),
				responseFields(
					fieldWithPath("roomTsid").description("채팅방 TSID"),
					fieldWithPath("lastReadMessageTsid").description("현재 읽음 위치 (뒤로 가는 요청이면 기존 값)"),
					fieldWithPath("updatedAt").description("갱신 일시")
				)
			));
	}

	private ChatMessageResponse message(String content) {
		return ChatMessageResponse.builder()
			.messageTsid(MESSAGE_TSID)
			.roomTsid(ROOM_TSID)
			.messageType(ChatMessageType.TEXT)
			.sender(ChatSenderSummary.builder().userTsid(USER_TSID).nickname("러닝맨").name("김병채")
				.profileImageUrl("https://example.com/me.jpg").withdrawn(false).build())
			.content(content)
			.createdAt(Instant.now())
			.build();
	}

	private org.springframework.restdocs.payload.FieldDescriptor[] messageFields(String prefix) {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
			fieldWithPath(prefix + "messageTsid").description("메시지 TSID (시간순, 커서로 사용)"),
			fieldWithPath(prefix + "roomTsid").description("채팅방 TSID"),
			fieldWithPath(prefix + "messageType").description("TEXT | SYSTEM"),
			fieldWithPath(prefix + "sender").description("발신자 (SYSTEM 메시지면 null)").optional(),
			fieldWithPath(prefix + "sender.userTsid").description("발신자 TSID"),
			fieldWithPath(prefix + "sender.nickname").description("발신자 닉네임").optional(),
			fieldWithPath(prefix + "sender.name").description("발신자 이름 (탈퇴 시 '탈퇴한 사용자')"),
			fieldWithPath(prefix + "sender.profileImageUrl").description("발신자 프로필 이미지 URL").optional(),
			fieldWithPath(prefix + "sender.withdrawn").description("탈퇴한 사용자 여부"),
			fieldWithPath(prefix + "content").description("메시지 내용"),
			fieldWithPath(prefix + "createdAt").description("생성 일시")
		};
	}
}
