# 채팅 실시간 수신 프로토콜 (클라이언트용)

근거와 대안 비교는 [ADR-0003](adr/0003-chat-realtime-delivery.md). 이 문서는 클라이언트가 구현해야 할 것만 적는다.

## 한 줄 요약

**보내는 건 REST, 받는 건 WebSocket(STOMP) 구독.** 소켓이 끊기면 REST `after` 조회로 빈틈을 메운다.

## 1. 접속

```
WebSocket  ws://{host}/ws
STOMP      CONNECT
           Authorization: Bearer {accessToken}
```

- 핸드셰이크(`/ws`)에는 인증이 없다 — 브라우저 WebSocket API 가 헤더를 못 붙이기 때문. 인증은 **CONNECT 프레임의 `Authorization` 네이티브 헤더**로 한다.
- 토큰이 없거나 만료되면 CONNECT 가 거부된다 (ERROR 프레임, 아래 표).

## 2. 구독

```
SUBSCRIBE  /topic/rooms/{roomTsid}
```

- `roomTsid` 는 모임/일정 상세 응답의 `chatRoomTsid`.
- 그 모임/일정의 **참여자만** 구독할 수 있다. 아니면 ERROR 프레임을 받고 연결이 닫힌다.
- 다른 목적지는 없다. `/topic/rooms/…` 외의 구독은 거부된다.

## 3. 전송

```
POST /chat-rooms/{roomTsid}/messages
{ "content": "…" }
```

STOMP `SEND` 는 받지 않는다. REST 응답과 구독으로 받는 MESSAGE 프레임의 본문은 **같은 JSON**(`ChatMessageResponse`)이다.

## 4. 수신

```
MESSAGE  destination: /topic/rooms/{roomTsid}
{
  "messageTsid": "0K3…",  "roomTsid": "…", "messageType": "TEXT",
  "sender": { "userTsid": "…", "nickname": "…", "name": "…", "profileImageUrl": "…", "withdrawn": false },
  "content": "…", "createdAt": "2026-09-14T09:00:00Z"
}
```

- 메시지는 **DB 에 커밋된 뒤에만** 도착한다. 도착한 메시지는 이미 저장된 것이다.
- `messageTsid` 는 시간순이라 정렬 키이자 커서다. 클라이언트는 **방마다 마지막으로 받은 `messageTsid`** 를 기억한다.

## 5. 재접속

소켓은 끊긴다(네트워크 전환, 백그라운드). 다시 CONNECT + SUBSCRIBE 한 뒤:

```
GET /chat-rooms/{roomTsid}/messages?after={마지막으로 받은 messageTsid}
```

- 오래된 순으로 돌아온다. `hasNext=true` 면 `nextCursor` 를 `after` 로 넘겨 반복.
- 구독을 먼저 하고 `after` 조회를 나중에 하면 빈틈이 생기지 않는다 (겹치는 메시지는 `messageTsid` 로 중복 제거).

## 6. 오류

ERROR 프레임의 `message` 헤더가 `ErrorCode` 이름이다.

| message | 언제 |
|---|---|
| `ACCESS_TOKEN_MISSING` | CONNECT 에 Authorization 없음 / 인증 없이 SUBSCRIBE |
| `ACCESS_TOKEN_EXPIRED` | 토큰 만료 → `/refresh` 후 재접속 |
| `ACCESS_TOKEN_MALFORMED` | 토큰 형식 오류 |
| `CHAT_ROOM_NOT_FOUND` | 없는 방, 또는 `/topic/rooms/{tsid}` 규약이 아닌 목적지 |
| `CHAT_ROOM_ACCESS_DENIED` | 그 방의 참여자가 아님, 또는 STOMP SEND 시도 |

서버는 ERROR 프레임 뒤 연결을 닫는다. 클라이언트는 원인에 따라 토큰을 갱신하거나 구독을 포기한다.

## 7. 아직 없는 것

타이핑 표시, 접속 상태, 메시지별 읽음('1'), 모임 탈퇴 시 살아 있는 구독 강제 종료(다음 재접속 때 거부됨).
