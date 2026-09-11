# ADR-0002. 채팅 메시지 저장소 — Apache Cassandra

- 상태: 채택
- 날짜: 2026-09-11
- 관련: #17 (채팅방 테이블 설계), #18 (채팅 전송), ADR-0003 (실시간 전송), ADR-0004 (멤버십)

## 맥락

채팅 메시지는 다른 도메인 데이터와 성격이 다르다.

- **append-only**: 수정·삭제가 거의 없고 쓰기가 계속 쌓인다
- **조회 패턴이 둘뿐**: "이 방의 최근 N건", "이 메시지 이후의 것들"
- **볼륨이 가장 큰 테이블**이 된다: 모임 1,000개가 하루 100건씩 주고받으면 연 3,650만 건

그리고 이 프로젝트에는 **학습 목표**가 있다. 팀에 NoSQL 운영 경험이 없고, 채팅은 Cassandra 계열이 정석으로 쓰이는 사례(디스코드 → Cassandra/ScyllaDB, 라인 → HBase)라 "언제 RDB 를 벗어나는가, 벗어나면 무엇을 잃는가"를 실제 코드로 겪어보기에 가장 적합한 도메인이다.

## 검토한 대안

| 방식 | 판단 | 이유 |
|---|---|---|
| 기존 MySQL (`chat_messages` 테이블) | 기각 (현 규모에선 충분) | 정합성이 한 트랜잭션 안에 있고 FK 가 삭제 순서를 강제하며 인프라가 늘지 않는다. **규모만 보면 이쪽이 맞다** — InnoDB 수천만 건은 `(room_tsid, tsid)` 인덱스 하나로 무난하다. 아래 "결정"의 근거 1 이 없었다면 이것을 택했을 것이다. 되돌릴 때의 목적지이기도 하다. |
| **Apache Cassandra** | **채택** | 아래 "결정" 참고 |
| ScyllaDB | 보류 | Cassandra 와 CQL 호환이라 코드는 그대로다. 로컬 기동이 빠르고(수 초) 자원을 덜 쓰지만, 학습 대상은 Cassandra 본체이고 운영 문서·커뮤니티도 그쪽이 많다. 로컬 자원이 문제되면 개발 환경만 교체할 수 있다. |
| MongoDB | 기각 | 채팅 스키마는 고정적이라 문서 유연성의 이점이 없다. 파티션 키 기반 시간순 조회는 Cassandra 의 클러스터링 컬럼이 정확히 그 용도다. |
| Redis (List / Stream) | 기각 (1차 저장소로) | 메모리가 곧 저장 한계. 최근 N건 캐시나 실시간 팬아웃(ADR-0003)에는 나중에 쓸 수 있다. |
| DynamoDB | 기각 | 데이터 모델은 유사하지만 AWS 종속. 로컬 개발·테스트가 에뮬레이터에 기댄다. |

## 결정

**채팅 메시지와 읽음 위치는 Apache Cassandra 에, 채팅방 메타데이터(`chat_rooms`)는 MySQL 에 둔다.**

근거:

1. **학습 가치.** 이 결정의 1차 근거다. 규모만으로는 정당화되지 않는다는 점을 숨기지 않는다. 대신 아래 "잃는 것"을 실제로 겪고 대응하는 것 자체가 목표다.
2. **채팅이 Cassandra 데이터 모델에 정확히 맞는다.** 파티션 키로 방을 고르고 클러스터링 컬럼으로 시간순 정렬 — "방의 최근 N건"이 한 파티션 안의 순차 읽기다. 쓰기는 LSM 트리(memtable → SSTable) 라 append-only 워크로드에 최적이다.
3. **되돌리기가 어렵지 않다.** 조회 패턴이 둘뿐이라 리포지토리 인터페이스가 작다. MySQL 로 돌아갈 때 서비스·도메인은 유지되고 리포지토리 구현과 인프라만 바뀐다.

### 무엇을 잃는가 — 그리고 어떻게 대응하는가

| 잃는 것 | 대응 |
|---|---|
| **조인** — 보낸 사람 닉네임·프로필, 멤버십은 MySQL 에 있다 | 메시지에는 `sender_tsid` 만 저장. 응답 조립 시 보낸 사람 TSID 를 모아 MySQL `users` 를 `IN` 한 번으로 조회해 Map 으로 붙인다 (N+1 금지 원칙 그대로). 멤버십은 ADR-0004 대로 MySQL 참여자 테이블에서 판정한다. |
| **트랜잭션** — MySQL 과 Cassandra 를 한 트랜잭션으로 묶을 수 없다 | 메시지 전송은 (MySQL: 멤버십 검증 읽기) → (Cassandra: insert) 순서이고 MySQL 쪽은 쓰기가 없어 원자성이 필요 없다. 모임/일정 삭제는 MySQL 커밋 **후** Cassandra 파티션을 best-effort 로 삭제한다. 실패하면 고아 파티션이 남지만, 방(`chat_rooms`)이 없어 접근 경로가 없으므로 무해한 쓰레기다. 정리 배치는 필요해질 때 만든다. |
| **FK** — `sender_tsid`, `room_tsid` 의 참조 무결성을 DB 가 지켜주지 않는다 | 애플리케이션이 지킨다: 저장 전 방 존재·멤버십을 MySQL 에서 검증한다. 회원 탈퇴(#48) 시 메시지의 `sender_tsid` 는 그대로 두고 응답 조립에서 "탈퇴한 사용자"로 표시한다 — 메시지 자체는 방의 기록이므로 지우지 않는다. |
| **파티션 크기 상한** — 한 파티션이 100MB / 수백만 셀을 넘으면 압축·리페어·읽기 성능이 급격히 나빠진다 | **시간 버킷**: 파티션 키를 `(room_tsid, bucket)` 으로 하고 `bucket = 'yyyy-MM'`. 방 하나가 아무리 활발해도 파티션은 한 달치로 잘린다. 커서 페이징은 현재 버킷에서 부족하면 이전 달 버킷으로 넘어간다. |
| **읽기 전 쓰기(read-before-write) 금지 관례** — `SELECT` 후 조건부 `UPDATE` 는 분산 환경에서 경쟁한다 | 읽음 위치는 "뒤로 가지 않는다" 규칙이 있지만, 단순 `INSERT`(upsert) 로 마지막 값을 덮어쓰고 클라이언트가 단조 증가 값을 보낸다고 가정한다. 엄격한 단조성이 필요해지면 LWT(`IF last_read < ?`)를 쓰되 비용(Paxos 왕복)을 인지한다. MVP 에서는 쓰지 않는다. |
| **집계** — `COUNT` 는 파티션 전체 스캔이다 | "안 읽은 N개"는 `(room, bucket)` 파티션 안에서 `message_tsid > last_read` 범위 `COUNT` 로 계산한다. 한 달치 파티션 안의 범위라 감당 가능하고, 100 을 넘으면 `99+` 로 표시해 `LIMIT 101` 로 끊는다. |
| **인프라·테스트** — 로컬 docker-compose 에 Cassandra 추가, 통합 테스트는 Testcontainers(Docker 필요) | 개발 환경은 이미 docker-compose(MySQL, Redis)에 기대고 있어 추가 부담이 작다. 테스트는 JVM 당 컨테이너 하나를 공유해 기동 비용(30초↑)을 한 번만 낸다. CI(#34) 설계 시 Docker-in-Docker 또는 서비스 컨테이너를 고려한다. |

### 데이터 모델 (query-first)

Cassandra 는 "어떻게 조회할 것인가"에서 출발해 테이블을 설계한다. 조회 하나에 테이블 하나가 원칙이다.

```sql
-- Q1. 방의 최근 N건 / 커서 이후 N건 (ADR-0003 의 after 보충 조회 포함)
CREATE TABLE chat_messages_by_room (
    room_tsid     text,
    bucket        text,          -- 'yyyy-MM', message_tsid 의 시각에서 도출
    message_tsid  text,          -- TSID, 시간순 정렬 키이자 커서
    sender_tsid   text,          -- SYSTEM 메시지는 null
    message_type  text,          -- TEXT | SYSTEM
    content       text,
    created_at    timestamp,
    PRIMARY KEY ((room_tsid, bucket), message_tsid)
) WITH CLUSTERING ORDER BY (message_tsid DESC);

-- Q2. 사용자의 (방별) 읽음 위치 — 내 채팅방 목록의 안 읽은 배지, 방 입장 시 구분선
CREATE TABLE chat_room_read_positions_by_user (
    user_tsid              text,
    room_tsid              text,
    last_read_message_tsid text,
    updated_at             timestamp,
    PRIMARY KEY ((user_tsid), room_tsid)
);
```

- `message_tsid` 를 `timeuuid` 가 아니라 TSID 문자열로 두는 이유: 애플리케이션 전체가 TSID 를 쓰고, Crockford base32 고정 길이라 문자열 비교가 시간순과 일치한다. 커서 의미가 다른 도메인과 같다.
- `bucket` 은 `message_tsid` 에서 계산할 수 있지만 파티션 키이므로 컬럼으로 둔다. 애플리케이션이 계산해 넣으며, 값 객체(`ChatMessageBucket`)가 이 규칙을 한 곳에 가둔다.
- `created_at` 은 TSID 와 중복 정보다. 사람이 읽기 위한 편의 컬럼이며 정렬에는 쓰지 않는다.
- 스키마는 개발 환경에서 `spring.cassandra.schema-action=CREATE_IF_NOT_EXISTS` 로 엔티티에서 생성한다 (MySQL 의 `ddl-auto: create` 와 같은 위치). 키스페이스는 docker-compose 초기화 컨테이너가 만든다.

## JPA 와 병행할 때 지켜야 할 것

한 애플리케이션에서 JPA(MySQL)와 Spring Data Cassandra 를 함께 쓰면 다음 지점에서 서로 다르게 동작한다.
각 항목은 `CassandraJpaCoexistenceTest` 로 실제 동작을 확인했다.

| 지점 | 무엇이 다른가 | 규칙 |
|---|---|---|
| **트랜잭션** | `@Transactional` 은 JPA 트랜잭션 매니저의 것이다. Cassandra 리포지토리는 이를 무시하고 **즉시 실행**한다. JPA 트랜잭션이 롤백돼도 그 안에서 한 Cassandra 쓰기는 남는다 (테스트로 재현). | MySQL **쓰기**가 있는 트랜잭션 안에서는 Cassandra 에 쓰지 않는다. 필요하면 커밋 후(`@TransactionalEventListener(AFTER_COMMIT)`)로 뺀다. 메시지 전송처럼 MySQL 쪽이 읽기뿐이면 같은 메서드에서 해도 된다 — 롤백될 것이 없다. |
| **변경 감지** | JPA 는 영속성 컨텍스트가 dirty checking 으로 변경을 flush 한다. Cassandra 엔티티는 영속성 컨텍스트가 없어 **수정 후 `save()` 를 호출해야** 반영된다 (테스트로 재현). | Cassandra 엔티티의 상태 변경 메서드(`advanceTo` 등)를 호출한 뒤에는 반드시 `repository.save()` 를 부른다. 엔티티 javadoc 에 명시한다. |
| **save 의 의미** | JPA `save` 는 insert/update 를 구분한다. Cassandra `save` 는 항상 **upsert**(`INSERT` 는 같은 키를 덮어쓴다). "이미 있으면 실패" 가 필요하면 LWT(`IF NOT EXISTS`)를 써야 하고 비용이 크다. | 메시지 TSID 는 앱이 유일하게 발급하므로 충돌이 없다. 읽음 위치는 덮어쓰는 것이 의도다. |
| **리포지토리 배정** | 두 Spring Data 모듈이 있으면 *strict repository configuration mode* 로 들어가, 리포지토리가 확장한 인터페이스(`JpaRepository` / `CassandraRepository`)로 모듈을 정한다. 기동 로그의 "Could not safely identify store assignment" 는 각 모듈이 남의 후보를 거르며 남기는 INFO 이며 오배정이 아니다. | 리포지토리는 반드시 모듈별 기반 인터페이스를 확장한다. 리액티브 Cassandra 스캔은 쓰지 않으므로 `spring.data.cassandra.repositories.type=imperative` 로 끈다. |
| **엔티티 어노테이션** | JPA `@Entity`/`jakarta.persistence.Table` 과 Cassandra `org.springframework.data.cassandra.core.mapping.Table` 은 다른 타입이라 서로의 스키마 생성에 섞이지 않는다. | 같은 패키지에 두어도 되지만, import 실수를 막기 위해 Cassandra 엔티티는 클래스 주석에 "(Cassandra: 테이블명)" 을 적는다. |
| **Auditing** | `@EnableJpaAuditing` 은 JPA 메타모델을 요구해 `@DataCassandraTest` 슬라이스에서 실패한다. | `JpaAuditingConfig` 로 분리했고 `@DataJpaTest` 는 `@Import` 한다. Cassandra 엔티티는 `createdAt`/`updatedAt` 을 직접 채운다 (`@EnableCassandraAuditing` 은 두지 않는다). |
| **예외 번역** | Cassandra 드라이버 예외는 `CassandraExceptionTranslator` 가 `DataAccessException` 계열로 바꾼다. 타임아웃은 `QueryTimeoutException`, 연결 실패는 `CassandraConnectionFailureException`. ADR-0001 의 `PessimisticLockingFailureException` 핸들러는 해당 없다. | #18 에서 Cassandra 타임아웃/연결 실패를 503 으로 번역하는 핸들러를 추가한다. 쓰기 타임아웃은 **부분 적용됐을 수 있다**는 점을 응답 메시지에 반영하지 않는다(클라이언트는 재조회로 확인). |
| **기동 의존** | MySQL 과 마찬가지로 Cassandra 에 연결되지 않으면 애플리케이션이 뜨지 않는다. | 로컬은 docker-compose, 테스트는 Testcontainers. 타임아웃은 환경변수로 조정 가능하다. |
| **ID 발급** | JPA 는 Hibernate `@Tsid` 생성기, Cassandra 는 `TsidCreator` 를 직접 쓴다. 둘 다 같은 라이브러리의 기본 팩토리라 형식(13자 Crockford)과 단조성이 같다. | 커서 비교·정렬은 문자열 비교로 통일한다. |

## 전환 기준과 경로

이 결정은 규모가 아니라 학습이 근거이므로, **되돌릴 조건**을 명시한다.

| 관측 | 판단 |
|---|---|
| 운영 부담(리페어, 컴팩션, 노드 관리)이 팀 역량을 넘어선다 | MySQL 로 복귀. 리포지토리 구현 교체 + 데이터 이관 (버킷별 순회 → INSERT) |
| 조인 부재로 인한 애플리케이션 조립 코드가 도메인 로직을 침식한다 | 위와 같음. 그 전에 응답 조립을 별도 계층(assembler)으로 격리한다 |
| 반대로 채팅이 실제로 커져 MySQL 이었으면 파티셔닝이 필요했을 규모 | 결정 유지. 이때는 학습 근거가 규모 근거로 바뀐다 |

## 결과

- 채팅 도메인은 리포지토리 두 종류(JPA — `chat_rooms`, Cassandra — 메시지·읽음 위치)를 가진다. 서비스가 두 저장소를 조율하며, 쓰기 순서는 "MySQL 검증 → Cassandra 쓰기" 로 고정한다.
- 메시지 조회는 반드시 파티션 키 `(room_tsid, bucket)` 을 모두 지정한다. `ALLOW FILTERING` 은 금지.
- 응답 조립(보낸 사람 정보 붙이기)은 항상 `IN` 한 번으로 한다.
- 테스트 실행에 Docker 가 필요하다. 이 사실을 README 에 적는다.
