package com.gathering.chat.infra;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gathering.auth.infra.JwtTokenProvider;
import com.gathering.chat.application.ChatMessageService;
import com.gathering.chat.application.ChatRoomService;
import com.gathering.chat.domain.model.ChatMessageBucket;
import com.gathering.chat.domain.model.ChatRoomEntity;
import com.gathering.chat.domain.repository.ChatMessageRepository;
import com.gathering.chat.domain.repository.ChatRoomReadPositionRepository;
import com.gathering.chat.domain.repository.ChatRoomRepository;
import com.gathering.gathering.domain.model.GatheringCategory;
import com.gathering.gathering.domain.model.GatheringEntity;
import com.gathering.gathering.domain.model.GatheringParticipantEntity;
import com.gathering.gathering.domain.model.ParticipantRole;
import com.gathering.gathering.domain.repository.GatheringParticipantRepository;
import com.gathering.gathering.domain.repository.GatheringRepository;
import com.gathering.region.domain.model.RegionEntity;
import com.gathering.region.domain.repository.RegionRepository;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.domain.repository.UsersRepository;

/**
 * 실시간 채팅 통합 테스트 — 실제 서버에 STOMP 클라이언트로 붙어 "REST 로 보내면 구독자에게 도착한다" 를 끝까지 검증한다 (docs/adr/0003)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatRealtimeIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private SimpleBrokerMessageHandler brokerMessageHandler;

	@Autowired
	private ChatMessageService chatMessageService;

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private RegionRepository regionRepository;

	@Autowired
	private GatheringRepository gatheringRepository;

	@Autowired
	private GatheringParticipantRepository participantRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatRoomReadPositionRepository readPositionRepository;

	private UsersEntity member;
	private UsersEntity stranger;
	private RegionEntity region;
	private GatheringEntity gathering;
	private ChatRoomEntity room;
	private WebSocketStompClient stompClient;

	@BeforeEach
	void setUp() {
		member = usersRepository.save(user("member"));
		stranger = usersRepository.save(user("stranger"));
		region = regionRepository.save(RegionEntity.builder()
			.name("실시간 테스트 지역").code("RT" + System.nanoTime() % 100000).depth(1).path("/rt/").build());
		gathering = gatheringRepository.save(GatheringEntity.builder()
			.name("실시간 테스트 모임").regionTsid(region.getTsid()).category(GatheringCategory.SPORTS).build());
		participantRepository.save(GatheringParticipantEntity.builder()
			.gatheringTsid(gathering.getTsid()).userTsid(member.getTsid()).role(ParticipantRole.OWNER).build());
		room = chatRoomService.createForGathering(gathering.getTsid());

		stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
	}

	@AfterEach
	void tearDown() {
		stompClient.stop();
		transactionTemplate.executeWithoutResult(status -> {
			chatRoomRepository.deleteAllByTsidIn(List.of(room.getTsid()));
			participantRepository.deleteAllByGatheringTsid(gathering.getTsid());
			gatheringRepository.delete(gathering);
			regionRepository.delete(region);
			usersRepository.deleteAll(List.of(member, stranger));
		});
		chatMessageRepository.deleteByKeyRoomTsidAndKeyBucket(room.getTsid(), ChatMessageBucket.of(Instant.now()).getValue());
		// 전송이 발신자의 읽음 위치를 만든다
		readPositionRepository.deleteByKeyUserTsid(member.getTsid());
	}

	@Test
	@DisplayName("멤버가 JWT 로 CONNECT 하고 방을 구독하면 REST 로 보낸 메시지가 저장 후 도착한다")
	void memberReceivesBroadcastAfterSend() throws Exception {
		// given: 멤버로 접속해 방을 구독
		FrameCollector frames = new FrameCollector();
		StompSession session = connect(member, frames);
		subscribeAndAwaitRegistration(session, ChatDestinations.room(room.getTsid()), frames);

		// when: 전송은 REST(서비스) 로
		chatMessageService.sendMessage(room.getTsid(), member.getTsid(), "실시간으로 도착");

		// then: 구독자에게 REST 조회와 같은 형태로 도착
		ReceivedMessage received = frames.messages.poll(10, TimeUnit.SECONDS);
		assertThat(received).isNotNull();
		assertThat(received.content()).isEqualTo("실시간으로 도착");
		assertThat(received.roomTsid()).isEqualTo(room.getTsid());
		assertThat(received.sender().name()).isEqualTo("member");
		session.disconnect();
	}

	@Test
	@DisplayName("토큰 없이 CONNECT 하면 접속이 거부된다")
	void connectWithoutTokenIsRejected() {
		StompHeaders connectHeaders = new StompHeaders();

		assertThatThrownBy(() -> stompClient
			.connectAsync(url(), new WebSocketHttpHeaders(), connectHeaders, new FrameCollector())
			.get(10, TimeUnit.SECONDS))
			.isInstanceOf(ExecutionException.class);
	}

	@Test
	@DisplayName("멤버가 아닌 사용자가 방을 구독하면 ERROR 프레임(CHAT_ROOM_ACCESS_DENIED)을 받고 메시지를 받지 못한다")
	void strangerSubscriptionIsRejected() throws Exception {
		// given: 인증은 되지만 모임 참여자가 아닌 사용자
		FrameCollector frames = new FrameCollector();
		StompSession session = connect(stranger, frames);

		// when
		session.subscribe(ChatDestinations.room(room.getTsid()), frames);

		// then
		StompHeaders error = frames.errors.poll(10, TimeUnit.SECONDS);
		assertThat(error).isNotNull();
		assertThat(error.getFirst("message")).isEqualTo("CHAT_ROOM_ACCESS_DENIED");
	}

	private StompSession connect(UsersEntity user, FrameCollector handler) throws Exception {
		StompHeaders connectHeaders = new StompHeaders();
		connectHeaders.add("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user.getTsid()));
		return stompClient.connectAsync(url(), new WebSocketHttpHeaders(), connectHeaders, handler)
			.get(10, TimeUnit.SECONDS);
	}

	/**
	 * SUBSCRIBE 는 응답이 없고 Simple Broker 는 RECEIPT 도 보내지 않아, 클라이언트는 브로커 등록 시점을 알 수 없다.
	 * 고정 sleep 은 느린 환경에서 구독 전에 브로드캐스트가 나가 메시지를 잃을 수 있으므로,
	 * 브로커의 구독 레지스트리(브로드캐스트가 실제로 참조하는 곳)에 목적지가 등록될 때까지 기다린다
	 */
	private void subscribeAndAwaitRegistration(StompSession session, String destination, StompFrameHandler handler)
		throws Exception {
		session.subscribe(destination, handler);

		SimpMessageHeaderAccessor probeHeaders = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		probeHeaders.setDestination(destination);
		Message<byte[]> probe = MessageBuilder.createMessage(new byte[0], probeHeaders.getMessageHeaders());
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			if (!brokerMessageHandler.getSubscriptionRegistry().findSubscriptions(probe).isEmpty()) {
				return;
			}
			Thread.sleep(20);
		}
		fail("브로커에 구독이 등록되지 않았다: " + destination);
	}

	private String url() {
		return "ws://localhost:" + port + ChatWebSocketConfig.ENDPOINT;
	}

	private UsersEntity user(String prefix) {
		return UsersEntity.builder().email(prefix + "-" + System.nanoTime() + "@example.com").name(prefix).build();
	}

	/** 브로드캐스트 페이로드 (ChatMessageResponse 와 같은 JSON) — 테스트에서 역직렬화하기 위한 record */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record ReceivedMessage(String messageTsid, String roomTsid, String content, ReceivedSender sender) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ReceivedSender(String userTsid, String name, boolean withdrawn) {
	}

	/** 세션 핸들러 겸 구독 프레임 핸들러 — 메시지와 ERROR 프레임을 큐에 모은다 */
	static class FrameCollector extends StompSessionHandlerAdapter implements StompFrameHandler {

		final BlockingQueue<ReceivedMessage> messages = new LinkedBlockingQueue<>();
		final BlockingQueue<StompHeaders> errors = new LinkedBlockingQueue<>();

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return ReceivedMessage.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			// 서버가 보내는 ERROR 프레임은 본문이 없어 payload 가 null 로 온다 (message 헤더에 ErrorCode 이름)
			if (payload == null) {
				errors.add(headers);
				return;
			}
			if (payload instanceof ReceivedMessage message) {
				messages.add(message);
			}
		}

		@Override
		public void handleException(StompSession session, StompCommand command, StompHeaders headers,
			byte[] payload, Throwable exception) {
			errors.add(headers);
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			// 서버가 ERROR 프레임 뒤 연결을 닫으면 여기로도 온다 — 이미 errors 에 기록됐다
		}
	}
}
