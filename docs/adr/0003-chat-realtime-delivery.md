# ADR-0003. 채팅 실시간 전송 — WebSocket + STOMP, 전송은 REST, 브로커는 단계적 확장

- 상태: 채택
- 날짜: 2026-09-11
- 관련: #18 (채팅 전송), ADR-0002 (저장소), ADR-0001 (락 안 외부 I/O 금지)

## 맥락

채팅의 핵심 UX 는 "보내면 바로 보인다" 이다. 이를 위한 서버→클라이언트 푸시 방식은 여러 가지이고,
선택에 따라 인증 방식, 다중 인스턴스 구성, 클라이언트 구현이 달라진다.
테이블 설계(ADR-0002)에는 영향이 없지만, 메시지 전송 흐름과 API 형태를 결정하므로 미리 정한다.

현재 환경: Spring Boot 3.5 / JWT 인증 / Redis 있음 / Thymeleaf 서버 렌더링 화면 / 인스턴스 1개 (Docker).

## 검토한 대안

| 방식 | 판단 | 이유 |
|---|---|---|
| HTTP short polling | 기각 | 가장 단순하고 인프라가 없지만, 지연이 폴링 주기(수 초)만큼 생기고 조용한 방에도 요청이 계속 간다. 채팅 UX 의 핵심을 포기하는 선택. |
| HTTP long polling | 기각 | 폴링 지연은 줄지만 서블릿 스레드를 붙잡아 두므로 비동기 처리(`DeferredResult`)가 필요하고, 결국 WebSocket 을 손으로 흉내 내는 셈이다. |
| SSE (Server-Sent Events) | 기각 (근접 대안) | 서버→클라 단방향 스트림. HTTP 라 프록시·인증이 단순하고 채팅 수신에는 충분하다. 전송은 REST 로 하면 된다. 다만 브라우저당 HTTP/1.1 연결 수 제한(6개)에 걸리기 쉽고, 타이핑 표시·접속 상태처럼 **양방향**이 필요해지면 채널을 하나 더 열어야 한다. Spring 의 STOMP 지원(구독 라우팅, 인터셉터, 보안)이 이미 갖춰져 있어 총 구현량이 SSE 와 비슷하거나 적다. |
| WebSocket (STOMP 없이 raw) | 기각 | 메시지 형식, 구독/해지, 라우팅을 직접 설계해야 한다. STOMP 가 그 프로토콜을 표준으로 제공한다. |
| **WebSocket + STOMP** | **채택** | Spring 표준(`@EnableWebSocketMessageBroker`). `/topic/rooms/{roomTsid}` 구독 모델이 채팅방과 1:1 로 맞는다. `ChannelInterceptor` 로 CONNECT/SUBSCRIBE 단계에서 JWT 검증과 멤버십 검증을 걸 수 있다. |
| 외부 실시간 서비스 (Pusher, Ably, Firebase) | 기각 | 구현은 가장 빠르지만 비용이 연결 수에 비례하고 종속이 생긴다. 자체 운영 가능한 규모다. |

## 결정

### 1. 수신은 WebSocket(STOMP) 구독, 전송은 REST

```
클라이언트                       서버
  │── STOMP CONNECT (JWT) ───────▶ ChannelInterceptor: 토큰 검증, Principal 설정
  │── SUBSCRIBE /topic/rooms/{id}▶ 멤버십 검증 (ADR-0004), 실패 시 구독 거부
  │
  │── POST /chat-rooms/{id}/messages ▶ @Transactional: 멤버십 검증 → DB 저장 → 커밋
  │                                     └─ AFTER_COMMIT: /topic/rooms/{id} 로 브로드캐스트
  │◀── MESSAGE ─────────────────────┘
```

**전송을 STOMP SEND 가 아니라 REST 로 하는 이유**: 검증 실패(400/403), 정원·권한 오류를 기존 `ErrorResponse` 형식 그대로 돌려줄 수 있고, REST Docs 에 문서화되며, 컨트롤러 테스트 방식이 같다. STOMP SEND 는 오류를 ERROR 프레임으로 별도 처리해야 해서 클라이언트 코드가 갈라진다. 두 채널을 거치는 왕복 지연은 수십 ms 로 무시할 수 있다.

**DB 가 source of truth, 소켓은 알림 채널이다.** 저장이 커밋된 뒤에만 브로드캐스트한다(`@TransactionalEventListener(phase = AFTER_COMMIT)`). 반대로 하면 롤백된 메시지가 화면에 뜬다. 이 규칙은 ADR-0001 의 "락 안 외부 I/O 금지" 와 같은 원칙이다.

### 2. 놓친 메시지는 REST 커서 조회로 보충

소켓은 끊긴다(네트워크 전환, 탭 백그라운드). 재접속 시 클라이언트는 마지막으로 받은 메시지 TSID 로
`GET /chat-rooms/{id}/messages?after={tsid}` 를 호출해 빈틈을 메운다. 즉 **폴링용과 같은 조회 API 가 WebSocket 이 있어도 반드시 필요**하다.
TSID 가 시간순 PK 라 이 커서가 곧 정렬 키다 (ADR-0002).

### 3. 브로커는 단계적으로

| 단계 | 구성 | 조건 |
|---|---|---|
| **지금** | Spring Simple Broker (인메모리) | 인스턴스 1개. 외부 의존 없음 |
| 스케일아웃 시 | **Redis pub/sub 팬아웃** | 인스턴스 2개 이상. A 에 붙은 사용자가 보낸 메시지를 B 의 구독자도 받아야 한다 |

Redis 는 STOMP 를 말하지 않으므로 Spring 의 `StompBrokerRelay`(RabbitMQ/ActiveMQ 용)는 쓸 수 없다.
대신 "커밋 후 브로드캐스트" 지점을 인터페이스(`ChatMessageBroadcaster`)로 두고,
지금은 `SimpMessagingTemplate` 으로 로컬 브로커에 보내는 구현, 나중에는 Redis 채널에 publish 하고 각 인스턴스가 subscribe 해서
자기 로컬 브로커로 넘기는 구현으로 바꾼다. 서비스 코드는 그대로다.
RabbitMQ 를 새로 들이는 것보다 이미 있는 Redis 를 쓰는 편이 ADR-0001·0002 의 "인프라를 늘리지 않는다" 원칙과 맞는다.

### 4. 인증·인가

- **CONNECT**: `Authorization: Bearer …` 헤더를 `ChannelInterceptor` 에서 `JwtTokenProvider` 로 검증. 실패 시 연결 거부. HTTP 핸드셰이크 단계가 아니라 STOMP 프레임 단계에서 하는 이유는 브라우저 WebSocket API 가 커스텀 헤더를 못 보내기 때문이다.
- **SUBSCRIBE**: 목적지 `/topic/rooms/{roomTsid}` 의 방에 대해 멤버십(ADR-0004)을 검증. 멤버가 아니면 구독 거부. 모임 탈퇴 후에도 기존 구독이 살아 있을 수 있으므로, 탈퇴 시 해당 사용자의 구독을 끊는 것은 #18 구현 항목이다.
- 전송 권한은 REST 경로에서 기존 방식대로 검증한다.

### 5. 범위 밖 (MVP 이후)

타이핑 표시, 접속 상태(presence), 메시지별 읽음 표시('1'), 푸시 알림(#19 는 별개 채널).

## 결과

- #18 은 (a) REST 전송/조회 API, (b) STOMP 설정 + 인터셉터, (c) AFTER_COMMIT 브로드캐스터 세 부분으로 나뉜다. (a) 만으로도 폴링 클라이언트가 동작하므로 (a) 를 먼저 만들고 (b)(c) 를 얹는다.
- 클라이언트는 "구독 + REST 전송 + 재접속 시 after 커서 보충" 세 가지를 구현해야 한다.
- 브로드캐스트 구현체는 인터페이스 뒤에 두어 스케일아웃 시 서비스 코드를 건드리지 않는다.
