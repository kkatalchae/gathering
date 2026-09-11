### 개요
관심사가 맞는 사람들을 이어주는 커뮤니티 플랫폼

### 🛠️ 기술 스택

### Backend
| Category | Technology | Version |
|:---:|:---:|:---:|
| Language | <img src="https://img.shields.io/badge/Java-007396?style=flat&logo=OpenJDK&logoColor=white"/> | 21 |
| Framework | <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat&logo=Spring%20Boot&logoColor=white"/> | 3.5.4 |
| Database | <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=MySQL&logoColor=white"/> | 8.0 |
| Cache | <img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=Redis&logoColor=white"/> | - |

### DevOps & Tools
| Category | Technology |
|:---:|:---:|
| Containerization | <img src="https://img.shields.io/badge/Docker-2496ED?style=flat&logo=Docker&logoColor=white"/> |
| Documentation | <img src="https://img.shields.io/badge/Spring%20REST%20Docs-6DB33F?style=flat&logo=Spring&logoColor=white"/> |

### 로컬 실행

```bash
docker compose up -d   # MySQL, Redis, Cassandra(+키스페이스 초기화)
./gradlew bootRun
```

### 테스트

```bash
./gradlew test
```

통합 테스트는 [Testcontainers](https://testcontainers.com/)로 Cassandra 컨테이너를 띄우므로 **Docker 가 실행 중이어야 합니다.**
테스트 JVM 당 컨테이너를 한 번만 띄워 공유합니다 (`CassandraTestContainerLauncher`). 첫 실행은 이미지 pull 과 기동으로 1분 정도 걸립니다.

### 설계 결정 기록 (ADR)

기술 선택의 근거는 [docs/adr](docs/adr) 에 남깁니다. 정원 동시성 제어(0001), 채팅 저장소(0002), 실시간 전송(0003), 채팅방 멤버십(0004).
