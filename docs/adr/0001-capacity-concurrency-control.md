# ADR-0001. 정원(참여 인원 제한) 동시성 제어 — 부모 row 비관적 락

- 상태: 채택
- 날짜: 2026-09-11
- 관련: #16 (일정 참여), #50 (모임 참여 정원 경쟁), #51 코드 리뷰

## 맥락

모임(`gatherings.max_participants`)과 일정(`schedules.max_participants`)에는 정원이 있고,
참여 요청은 "현재 참여 인원 < 정원"일 때만 성공해야 한다. 이 불변식은

```
COUNT(*) FROM {gathering|schedule}_participants WHERE parent_tsid = ?  <=  parent.max_participants
```

처럼 **자식 row 의 집계**와 **부모 row 의 컬럼** 사이에 걸쳐 있다.

락 없이 `count → 비교 → insert` 로 구현하면, 남은 자리가 1인 곳에 두 요청이 동시에 오면
둘 다 같은 count 를 읽고 검사를 통과해 정원을 초과한다 (#50).
`uk(parent_tsid, user_tsid)` 는 *같은 사용자*의 중복만 막고, *다른 사용자*의 동시 참여는 막지 못한다.

정원을 줄이는 수정(`updateSchedule`)도 같은 불변식을 건드린다 — "현재 인원 <= 새 정원"을 검사한 뒤 정원을 바꾸므로,
동시에 참여가 들어오면 참여 4명 / 정원 3 같은 상태가 된다.

## 검토한 대안

| 방식 | 판단 | 이유 |
|---|---|---|
| **부모 row 비관적 락** (`SELECT … FOR UPDATE`) | **채택** | 같은 부모에 대한 쓰기를 DB 가 직렬화한다. 추가 인프라·컬럼·재시도 없음. 트랜잭션이 짧아(count + insert, ms 단위) 직렬화 비용이 사실상 없다. |
| 낙관적 락 (`@Version` on parent) | 기각 | 참여는 부모 row 를 **수정하지 않으므로** 버전이 바뀌지 않아 충돌을 감지할 수 없다. 감지하려면 참여 때마다 부모 row 를 강제로 갱신해야 하고, 그러면 결국 쓰기 락과 같으면서 재시도 루프만 늘어난다. 선착순처럼 한 row 에 요청이 몰리는 상황에서 낙관적 락은 재시도 폭주로 오히려 불리하다. |
| 카운터 컬럼 + 원자적 UPDATE (`UPDATE parent SET count = count + 1 WHERE count < max`) | 기각 | 가장 빠르지만 `participant_count` 라는 **파생 데이터**를 저장해야 한다. 참여/취소/삭제/회원탈퇴 모든 경로가 카운터를 정확히 유지해야 하고 한 번 어긋나면 되돌리기 어렵다. 이 프로젝트는 파생 데이터를 저장하지 않고 조회 시점에 계산하는 원칙을 따른다 (일정 참여자의 모임 소속 여부도 같은 이유로 조인 판정). |
| 애플리케이션 락 (`synchronized`, `ReentrantLock`) | 기각 | JVM 하나에서만 유효하다. Docker 로 인스턴스를 늘리는 순간 깨진다. |
| Redis 분산 락 (`SETNX`, Redisson) | 기각 | Redis 는 이미 있지만, DB 가 원자적으로 해주는 일을 밖으로 빼는 것이다. Redis 장애가 참여 실패로 번지고, 락 해제와 DB 커밋이 원자적이지 않아 그 사이에 창이 생긴다. TTL 튜닝도 필요하다. |
| `SERIALIZABLE` 격리 수준 | 기각 | MySQL 에서 일반 SELECT 가 공유 락 읽기로 바뀌어 두 트랜잭션이 S 락을 잡은 뒤 X 로 올리려다 서로 데드락에 빠지는 전형적 패턴이 생긴다. 범위가 너무 넓다. |
| 큐 — 메시지 큐(Kafka, Redis Streams) / 대기열(Redis Sorted Set) | 기각 (현 규모) | 큐는 정합성을 없애는 게 아니라 **옮긴다** — 컨슈머가 파티션당 하나일 때만 직렬화되고, 늘리면 DB 락이 다시 필요하다. 즉 큐는 부하 완충, 락은 정합성으로 서로 보완재다. 또한 동기 API(201/409 즉시 응답)가 "접수됨 → 나중에 확인"의 비동기로 바뀌어 클라이언트가 복잡해지고, 브로커라는 장애점이 늘어난다. 정원 수 명~100명 규모에서 그 대가를 치를 이유가 없다. 전환 기준은 아래 "큐로 전환하는 시점" 참고. |

### 큐로 전환하는 시점

다음 중 하나가 관측되면 DB 락은 그대로 둔 채 **앞단에 대기열을 붙이는** 형태로 확장한다.

- 락 대기 시간이 수십 ms 를 넘어 응답 지연으로 체감된다 (`innodb_row_lock_time_avg` 모니터링)
- 인기 일정에 정원 대비 100배 이상의 요청이 같은 시각에 몰린다 (티켓팅·수강신청 패턴)
- "엄격한 선착순 공정성"이 제품 요구가 된다 — InnoDB 락 대기는 대체로 도착 순이지만 보장은 아니다

이때도 정합성의 최종 방어선은 여전히 DB 락이다. 큐는 DB 앞의 완충 장치일 뿐, 컨슈머 수와 무관하게 정원 초과를 막는 것은 락이다.

## 결정

**정원 불변식을 건드리는 쓰기 경로는 모두 부모 row 의 X 락을 먼저 잡는다.**

| 경로 | 락 | 이유 |
|---|---|---|
| `joinGathering`, `joinSchedule` | 부모 row `FOR UPDATE` | 불변식 보장 (count → insert 를 직렬화) |
| `updateSchedule` | 일정 row `FOR UPDATE` | 정원 축소 검증이 참여와 직렬화되어야 함 |
| `deleteSchedule`, `deleteGathering`, `deleteSchedulesByGatheringTsid` | 부모 row(들) `FOR UPDATE` | 불변식과 무관하지만 **락 순서 통일**로 데드락 방지 (아래) |
| `leaveGathering`, `leaveSchedule` | 락 없음 | 인원이 줄어드는 방향이라 불변식을 깨지 못하고, PK 삭제라 갭 락도 잡지 않는다 |

리포지토리 메서드는 `findByTsidForUpdate` 처럼 이름에 `ForUpdate` 를 붙여 락을 잡는다는 것을 호출부에서 드러낸다.

## 데드락

데드락은 두 트랜잭션이 **서로 다른 순서**로 락을 잡을 때만 생긴다. 이 코드베이스에서 실제로 가능했던 시나리오:

```
J (참여):          X(schedule row) 획득 → … → INSERT participant (uk 인덱스 갭에 insert-intention 락 필요)
D (호스트 삭제):   [락 없이 조회] → DELETE participants WHERE schedule_tsid=? (범위 → 갭 락 획득)
                   → DELETE schedule (X(schedule row) 필요 → J 를 기다림)
J 의 INSERT 는 D 의 갭 락을 기다림  ⇒  순환 대기
```

해법은 **모든 경로가 부모 row 락을 먼저 잡는 것**이다. 그러면 D 는 첫 단계에서 J 를 기다리고, J 가 끝나야 진행하므로 순환이 생기지 않는다.
락 획득 순서는 항상 `모임 row → 일정 row(들) → 참여자 row` 로 고정한다. 모임 참여는 모임 row 만, 일정 참여는 일정 row 만 잡으므로 두 경로가 교차하지 않는다.

그래도 남는 경우(FK 검사 락 등 예측하기 어려운 조합)는 InnoDB 가 즉시 감지해 한쪽을 롤백한다(`innodb_deadlock_detect=ON` 기본값).
그 예외(`PessimisticLockingFailureException` — 데드락 패자, 락 대기 타임아웃 모두 포함)는 500 이 아니라
**409 `CONCURRENT_REQUEST_CONFLICT` "잠시 후 다시 시도"** 로 번역한다. 요청 자체는 유효하고 재시도하면 성공하기 때문이다.

락을 잡은 트랜잭션 안에서 지켜야 할 규칙:

- **외부 I/O 금지** — 파일 업로드, 외부 API 호출, 알림 전송(#19)을 락 안에서 하면 그 시간만큼 다른 참여가 멈춘다. 알림은 커밋 후(`@TransactionalEventListener(AFTER_COMMIT)` 등)로 뺀다.
- 락 범위는 PK 등호 조회(`WHERE tsid = ?`)로 **row 하나**만. 범위 조건 `FOR UPDATE` 는 갭 락을 동반하므로 피한다
  (`deleteSchedulesByGatheringTsid` 는 모임 단위로 일정 row 들을 잡지만, 그 모임의 일정 참여는 각 row 락에서 기다리므로 순서는 유지된다).

## 조회 성능

InnoDB 는 MVCC 를 쓰므로 **일반 `SELECT` 는 `FOR UPDATE` 락을 기다리지 않는다**. 스냅샷을 읽을 뿐이다.
따라서 목록(`GET /schedules`), 상세(`GET /schedules/{tsid}`), 참여 인원 집계 등 읽기 트래픽은 참여가 몰려도 영향을 받지 않는다.

기다리는 것은 **같은 부모 row 에 대한 다른 쓰기**뿐이며, 락 보유 시간은 `exists + count + insert` ≈ 수 ms 다.
같은 일정에 초당 수백 건의 참여가 몰려도 직렬화로 처리 가능한 수준이고, 실제 정원(수 명 ~ 100명)을 생각하면 한참 여유가 있다.

성능이 문제가 되는 조건은 딱 하나 — 락을 잡은 채 오래 머무는 것이다. 위 "외부 I/O 금지" 규칙이 그 방어선이다.
`innodb_lock_wait_timeout`(기본 50초) 은 그 규칙이 깨졌을 때의 마지막 안전장치이며, 운영 환경에서는 수 초로 낮추는 것을 권장한다.

## 검증

- `GatheringJoinConcurrencyTest`, `ScheduleJoinConcurrencyTest` — 실제 DB(H2) 에서 스레드 N 개가 동시에 참여를 시도해
  **정확히 정원만큼만 성공**하는지 확인한다. mock 으로는 잡을 수 없는 종류의 버그라 통합 테스트로 둔다.
- `@DataJpaTest` 로 `ForUpdate` 쿼리가 실제로 실행되는지 확인한다.

## 결과

- 정원 초과가 구조적으로 불가능해진다.
- 참여 경로는 부모 row 하나에서 직렬화되지만, 트랜잭션이 짧아 체감 지연은 없다.
- 새 쓰기 경로를 추가할 때는 "부모 row 락을 먼저 잡는가"를 확인해야 한다. 이 문서가 그 체크리스트다.
