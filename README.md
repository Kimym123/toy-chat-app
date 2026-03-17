# Real-Time Chat Application

STOMP 프로토콜 기반 실시간 채팅 API 서버입니다.
WebSocket을 통한 양방향 메시징, JWT 인증, 메시지 수정/삭제/복구, 타이핑 표시, 읽음 처리를 지원합니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.3 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Real-time | WebSocket + STOMP (SockJS 폴백) |
| ORM | Spring Data JPA + QueryDSL 5.1 |
| Database | H2 (개발) / MySQL 8.x (운영) |
| Docs | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Test | JUnit 5 + Spring Boot Test (85개 테스트) |
| Build | Gradle (Groovy DSL) |
| Cache | Redis (예정) |
| Infra | Docker Compose (예정) |
| CI | GitHub Actions (예정) |

## 주요 기능

### 실시간 메시징
- WebSocket + STOMP 프로토콜 기반 양방향 통신
- 타이핑 상태 실시간 표시
- 읽음 처리 및 안읽은 메시지 수 조회
- 입장/퇴장 시스템 메시지 자동 브로드캐스트

### 메시지 관리
- 메시지 수정/삭제/복구 (생성 후 5분 제한)
- 소프트 삭제 패턴 (물리적 삭제 없이 플래그 처리)
- 낙관적 락(`@Version`)으로 동시 수정 충돌 방지
- `clientMessageId`로 네트워크 재전송 시 중복 방지 (멱등성)

### 채팅방
- 1:1 채팅 (PRIVATE) / 그룹 채팅 (GROUP)
- 채팅방 참여자 관리 (초대, 나가기)
- 역할 기반 권한 (OWNER / MODERATOR / MEMBER)

### 인증/보안
- JWT 기반 Stateless 인증 (Access Token 30분 / Refresh Token 2주)
- WebSocket 핸드셰이크 시 JWT 검증 + 세션에 memberId 저장
- 메시지 발신자 서버 측 강제 설정 (발신자 위조 불가)
- CORS 도메인 제한 (REST + WebSocket)

## 아키텍처

```
back/src/main/java/org/example/back/
├── config/              # Security, WebSocket, CORS, QueryDSL, OpenAPI 설정
│   └── websocket/       # WebSocket 설정 + 핸드셰이크 인터셉터
├── controller/          # REST API + WebSocket STOMP 컨트롤러 (8개)
├── domain/              # 엔티티
│   ├── auth/            # RefreshToken
│   ├── base/            # BaseTimeEntity (createdAt, updatedAt)
│   ├── member/          # Member, MemberRole
│   ├── message/         # ChatMessage, MessageType, TypingStatus
│   └── room/            # ChatRoom, ChatParticipant, ChatRoomRole, ChatRoomType
├── dto/                 # 요청/응답 DTO (auth, member, message, room, websocket, file)
├── exception/           # 예외 처리
│   ├── base/            # CustomException, ErrorCode 인터페이스
│   ├── auth/            # AuthException, AuthErrorCode
│   ├── member/          # MemberException, MemberErrorCode
│   ├── chatroom/        # ChatRoomException, ChatRoomErrorCode
│   ├── message/         # ChatMessageException, ChatMessageErrorCode
│   ├── file/            # FileException, FileErrorCode
│   └── global/          # GlobalExceptionHandler
├── listener/            # STOMP 연결/해제 이벤트 리스너
├── repository/          # JPA + QueryDSL 리포지토리 (8개)
├── security/            # JwtTokenProvider, JwtAuthenticationFilter
├── service/             # 비즈니스 로직 (10개)
│   ├── message/         # ChatMessageService, ReadReceiptService
│   ├── room/            # ChatRoomService
│   └── file/            # FileStorageService
└── util/                # WebSocket 유틸리티
```

## ERD

```
[Member]                    [ChatRoom]
├── id (PK)                 ├── id (PK)
├── username (UNIQUE)       ├── type (PRIVATE/GROUP)
├── password (BCrypt)       ├── name
├── nickname (UNIQUE)       ├── isDeleted
├── email (UNIQUE)          ├── createdAt
├── phone                   └── updatedAt
├── profileImageUrl
├── role (USER/MODERATOR/ADMIN)
├── createdAt                   [ChatParticipant]
└── updatedAt                   ├── id (PK)
                                ├── member_id (FK → Member)
[ChatMessage]                   ├── chatRoom_id (FK → ChatRoom)
├── id (PK)                     ├── lastReadMessageId
├── chatRoom_id (FK)            ├── role (MEMBER/MODERATOR/OWNER)
├── sender_id (FK, nullable)    ├── createdAt
├── content (LOB)               └── updatedAt
├── messageType (TEXT/IMAGE/FILE/SYSTEM)
├── clientMessageId (UNIQUE)    [RefreshToken]
├── version (@Version)          ├── id (PK)
├── isDeleted                   ├── member_id (FK → Member)
├── createdAt                   ├── token (UNIQUE)
└── updatedAt                   ├── expiresAt
                                ├── createdAt
                                └── updatedAt
```

## API 엔드포인트

### 회원/인증

| Method | Endpoint | Description | 인증 |
|--------|----------|-------------|------|
| POST | `/api/members/register` | 회원가입 | X |
| POST | `/api/members/login` | 로그인 | X |
| GET | `/api/members/{id}` | 회원 정보 조회 (본인만) | O |
| PUT | `/api/members/{id}/password` | 비밀번호 변경 (본인만) | O |
| DELETE | `/api/members/{id}` | 회원 삭제 (본인만) | O |
| POST | `/api/token/refresh` | Access Token 재발급 | X |
| POST | `/api/logout` | 로그아웃 | X |

### 채팅방

| Method | Endpoint | Description | 인증 |
|--------|----------|-------------|------|
| POST | `/api/chat/rooms/private` | 1:1 채팅방 생성 | O |
| POST | `/api/chat/rooms/group` | 그룹 채팅방 생성 | O |
| GET | `/api/chat/rooms/my` | 내 채팅방 목록 (페이징) | O |
| POST | `/api/chat/rooms/{roomId}/invite` | 멤버 초대 | O |
| POST | `/api/chat/rooms/{roomId}/leave` | 채팅방 나가기 | O |
| DELETE | `/api/chat/rooms/{roomId}` | 채팅방 삭제 | O |

### 메시지

| Method | Endpoint | Description | 인증 |
|--------|----------|-------------|------|
| PUT | `/api/chat/message/{messageId}` | 메시지 수정 (5분 제한) | O |
| DELETE | `/api/chat/message/{messageId}` | 메시지 삭제 (5분 제한) | O |
| POST | `/api/chat/message/{messageId}/restore` | 메시지 복구 (5분 제한) | O |

### 읽음 상태

| Method | Endpoint | Description | 인증 |
|--------|----------|-------------|------|
| GET | `/api/chat/room/{chatRoomId}/read-status` | 채팅방 읽음 상태 조회 | O |
| GET | `/api/chat/room/{chatRoomId}/unread-count` | 안읽은 메시지 수 | O |

### 파일

| Method | Endpoint | Description | 인증 |
|--------|----------|-------------|------|
| POST | `/api/files/upload` | 파일 업로드 | O |

## WebSocket STOMP

### 연결
```
ws://localhost:8080/ws/chat?token={JWT_ACCESS_TOKEN}
```
SockJS 폴백을 지원하며, 핸드셰이크 시 JWT 토큰을 검증합니다.

### 메시지 흐름

```
Client                              Server
  │                                    │
  ├──── CONNECT /ws/chat?token=... ──►│  JWT 검증 + 세션에 memberId 저장
  │                                    │
  ├──── SUBSCRIBE ────────────────────►│
  │     /sub/chat/room/{roomId}        │
  │                                    │
  ├──── SEND /pub/chat/send ─────────►│  메시지 저장 + 브로드캐스트
  │                                    │
  │◄─── MESSAGE ───────────────────────┤  채팅방 참여자 전체에 전달
  │                                    │
```

### 발행 (Publish)

| Destination | 설명 | Payload |
|-------------|------|---------|
| `/pub/chat/send` | 메시지 전송 | chatRoomId, content, type, clientMessageId |
| `/pub/chat/read` | 읽음 처리 | memberId, chatRoomId, messageId |
| `/pub/chat/typing` | 타이핑 상태 | chatRoomId, typingStatus (TYPING/STOP) |

### 구독 (Subscribe)

| Destination | 설명 |
|-------------|------|
| `/sub/chat/room/{roomId}` | 새 메시지 + 읽음 알림 수신 |
| `/sub/chat/room/{roomId}/typing` | 타이핑 상태 수신 |
| `/sub/chat/room/{roomId}/delete` | 메시지 삭제 이벤트 수신 |
| `/sub/chat/room/{roomId}/restore` | 메시지 복구 이벤트 수신 |
| `/user/queue/errors` | STOMP 에러 응답 수신 (본인에게만 전달) |

## 설계 결정

### 왜 소프트 삭제 + 낙관적 락인가

채팅 메시지는 삭제 후에도 복구할 수 있어야 하므로 물리적 삭제 대신 `isDeleted` 플래그를 사용합니다.
동시에 같은 메시지를 수정하는 경우 `@Version` 필드로 충돌을 감지하고 409 Conflict를 반환합니다.

```java
// 낙관적 락 - 동시 수정 시 충돌 감지
@Version
private Long version;

// 소프트 삭제 - 복구 가능한 삭제
public void softDelete() {
    this.isDeleted = true;
}
```

### 왜 STOMP 프로토콜인가

WebSocket 위에 STOMP 프로토콜을 사용하여 발행/구독 모델을 구현했습니다.
순수 WebSocket 대비 메시지 라우팅, 채널 관리, 프레임 포맷이 표준화되어 클라이언트 구현이 단순해집니다.
Spring의 `SimpleMessageBroker`를 사용하며, 향후 멀티 인스턴스 확장 시 Redis Pub/Sub으로 전환할 수 있는 구조입니다.

### 왜 멱등성 키(clientMessageId)인가

네트워크 불안정 시 클라이언트가 같은 메시지를 재전송할 수 있습니다.
`clientMessageId`(UUID)를 DB에 UNIQUE 제약으로 걸어 중복 저장을 방지합니다.

### WebSocket 발신자 위조 방지

핸드셰이크에서 JWT를 검증하고 세션에 `memberId`를 저장한 뒤,
메시지 전송 시 클라이언트가 보낸 `senderId`를 무시하고 세션의 값으로 강제 설정합니다.

```java
// 핸드셰이크에서 검증된 세션 값으로 발신자 강제 설정
Long memberId = (Long) attributes.get("memberId");
request.setSenderId(memberId);
```

## 테스트

```bash
cd back
./gradlew test
```

| 분류 | 파일 수 | 테스트 수 | 전략 |
|------|---------|----------|------|
| Controller | 2 | 15 | `@WebMvcTest` + 서비스 모킹 |
| Service | 3 | 48 | `@SpringBootTest` + `@Transactional` |
| Repository | 2 | 14 | `@DataJpaTest` |
| Security | 1 | 8 | JWT 필터 테스트 |
| 기타 | 1 | 1 | 애플리케이션 로드 테스트 |
| **합계** | **9** | **86** | |

테스트 환경: H2 인메모리 (`create-drop` 모드)

## 실행 방법

### 사전 조건
- Java 21+
- Gradle 8+

### 로컬 실행

```bash
cd back

# 환경변수 설정
export JWT_SECRET=your-base64-encoded-secret-key

# 빌드 + 실행 (dev 프로필, H2 DB)
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html

### Docker Compose (예정)

```bash
docker compose up -d
```

## 개선 로드맵

### Phase 1: 보안/버그 수정 (진행중)

- [x] JWT Secret 환경변수 분리
- [x] MemberController 인가(Authorization) 검증 추가
- [x] WebSocket CORS 도메인 제한
- [x] REST CORS 프론트엔드 포트 추가
- [x] StompConnectListener 세션 기반 전환
- [x] AuthController 로그 토큰 마스킹
- [x] unread-count API 인가 처리
- [x] inviteMembers 권한 검증
- [x] DB 자격증명 환경변수 분리
- [x] FileUploadController 인증 + 파일 검증
- [x] RefreshToken 만료시간 검증
- [x] 페이지네이션 오류 수정
- [x] IllegalArgumentException → 도메인별 커스텀 예외 전환
- [x] ErrorResponse 실무 구조 개선 (code, timestamp, path 추가)
- [x] 로그 포맷 실무화 (에러코드, HTTP method, path, 4xx/5xx 레벨 구분)
- [x] STOMP 예외 처리 추가 (@MessageExceptionHandler + /user/queue/errors)
- [x] 범용 예외 잔존분 커스텀 예외 전환 (AccessDeniedException, IllegalArgumentException)
- [ ] 응답 형식/상태코드 통일

### Phase 2: Redis + Docker

- [ ] Redis Refresh Token 저장소 전환 (DB → Redis, TTL 자동 만료)
- [ ] 최근 메시지 캐싱 (Cache-Aside 패턴)
- [ ] Redis Pub/Sub 메시지 브로드캐스팅 (멀티 인스턴스 대응)
- [ ] Docker Compose (app + MySQL + Redis)
- [ ] 메시지 히스토리 조회 REST API 추가

### Phase 3: 테스트 + 성능

- [ ] ChatMessageService 단위 테스트 추가
- [ ] Redis 관련 테스트
- [ ] API 통합 테스트
- [ ] k6 부하 테스트 (동시 접속 500명 기준 p50/p95/p99 측정)

### Phase 4: 문서화 + CI

- [ ] GitHub Actions CI (PR 시 자동 테스트)
- [ ] 성능 테스트 결과 README 반영
- [ ] GitHub 프로필 정리

## 프로젝트 수치

| 항목 | 수량 |
|------|------|
| Java 소스 파일 | 90개 |
| 테스트 파일 | 9개 |
| 테스트 메서드 | 86개 |
| REST API 엔드포인트 | 19개 |
| WebSocket MessageMapping | 3개 |
| 엔티티 | 5개 |
| Repository | 8개 |
| Service | 10개 |
| Controller | 8개 |

## License

MIT License
