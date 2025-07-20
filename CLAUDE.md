# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 필요한 가이드를 제공합니다.

## 프로젝트 개요

Spring Boot 백엔드와 React 프론트엔드로 구성된 실시간 채팅 애플리케이션입니다. 백엔드는 STOMP 프로토콜을 사용한 WebSocket 메시징, JWT 인증을 구현하며, 메시지 수정/삭제/복구, 타이핑 상태 표시, 읽음 처리 등의 기능을 지원합니다.

## 개발 명령어

### 백엔드 (Spring Boot)
```bash
# 백엔드 디렉토리로 이동
cd back

# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "ChatMessageServiceTest"

# 클린 후 재빌드 (QueryDSL 이슈 해결용)
./gradlew clean build

# QueryDSL 생성 파일 정리
./gradlew cleanQuerydsl

# 애플리케이션 실행 (dev 프로필)
./gradlew bootRun
```

### 프론트엔드 (React + Vite)
```bash
# 프론트엔드 디렉토리로 이동
cd front

# 의존성 설치
npm install

# 개발 서버 시작
npm run dev

# 프로덕션 빌드
npm run build

# 린터 실행
npm run lint

# 프로덕션 빌드 미리보기
npm run preview
```

## 아키텍처 개요

### 백엔드 아키텍처

**핵심 기술 스택:**
- Spring Boot 3.4.3 with Java 21
- Spring Security with JWT 인증
- Spring WebSocket with STOMP 메시징
- JPA with QueryDSL (복잡한 쿼리용)
- H2 (개발) / MySQL (운영) 데이터베이스

**주요 아키텍처 패턴:**

1. **계층형 아키텍처:** Controller → Service → Repository → Domain
2. **STOMP WebSocket 메시징:** 구독/발행 모델을 통한 실시간 채팅
3. **소프트 삭제 패턴:** 메시지를 물리적으로 삭제하지 않고 삭제 표시
4. **낙관적 락:** ChatMessage의 version 필드로 동시 수정 충돌 방지
5. **이벤트 브로드캐스팅:** 메시지 작업(삭제/복구)에 대한 실시간 알림

**WebSocket 메시지 흐름:**
- 클라이언트가 `/ws/chat` 엔드포인트에 연결
- `/pub/chat/send`로 메시지 발행
- `/sub/chat/room/{roomId}`를 구독하여 메시지 수신
- `/sub/chat/room/{roomId}/delete`를 구독하여 삭제 이벤트 수신
- `/sub/chat/room/{roomId}/restore`를 구독하여 복구 이벤트 수신

**메시지 작업:**
- **생성:** clientMessageId를 통한 중복 방지 기능이 있는 기본 메시지 생성
- **수정:** 5분 제한 시간과 낙관적 락 적용
- **삭제:** 5분 제한 시간과 실시간 브로드캐스트가 있는 소프트 삭제
- **복구:** 5분 제한 시간 내 삭제 취소 및 실시간 브로드캐스트

**데이터베이스 설계:**
- `ChatMessage`는 소프트 삭제 지원 (`isDeleted` 필드)
- 메시지 타입: TEXT, IMAGE, FILE, SYSTEM
- QueryDSL 리포지토리가 자동으로 삭제된 메시지 필터링
- 낙관적 락으로 동시 수정 충돌 방지

### 프론트엔드 아키텍처

기본적인 React + TypeScript + Vite 설정입니다. 현재는 최소한의 구성이며, 주요 개발 집중도는 백엔드 WebSocket API와 실시간 메시징 인프라에 있습니다.

## 중요 설정 정보

**데이터베이스 프로필:**
- `dev` 프로필: H2 인메모리 데이터베이스, `ddl-auto: update`
- `prod` 프로필: MySQL, 제어된 스키마 관리

**JWT 설정:**
- 액세스 토큰: 30분 유효기간
- 리프레시 토큰: 2주 유효기간
- application.yml에 Base64 인코딩된 시크릿 키

**WebSocket 설정:**
- STOMP 엔드포인트: `/pub` (발행), `/sub` (구독), `/user` (1:1 메시징)
- 더 넓은 브라우저 지원을 위한 SockJS 폴백 활성화
- 핸드셰이크 인터셉터를 통한 JWT 토큰 인증

**QueryDSL 설정:**
- Q-클래스는 `build/generated/querydsl`에 생성
- 생성 충돌 해결을 위한 커스텀 태스크 `cleanQuerydsl`
- 소프트 삭제된 메시지를 필터링하는 복잡한 쿼리에 사용

## 주요 비즈니스 규칙

**메시지 시간 제약:**
- 수정 가능 시간: 생성 후 5분 이내
- 삭제 가능 시간: 생성 후 5분 이내
- 복구 가능 시간: 생성 후 5분 이내

**권한 모델:**
- 사용자는 본인 메시지만 수정/삭제/복구 가능
- 시스템 메시지(sender=null)는 삭제나 복구 불가
- 모든 메시지 작업은 채팅방 참여자들에게 실시간 이벤트 브로드캐스트

**메시지 상태 관리:**
- 삭제된 메시지는 "삭제된 메시지입니다."로 표시되며 SYSTEM 타입으로 변경
- QueryDSL 리포지토리가 일반 쿼리에서 삭제된 메시지를 자동으로 제외
- 복구 시 메시지가 원래 내용과 타입으로 되돌아감

## 테스트 전략

테스트는 계층별로 구성:
- **컨트롤러 테스트:** 서비스를 모킹한 `@WebMvcTest` 사용
- **서비스 테스트:** `@Transactional`과 함께 `@SpringBootTest` 사용
- **리포지토리 테스트:** 테스트 데이터와 함께 `@DataJpaTest` 사용
- **테스트 프로필:** `create-drop` DDL 모드의 인메모리 H2 데이터베이스

테스트 작업 시 주의사항: QueryDSL 쿼리를 사용하는 리포지토리 테스트를 실행하기 전에 QueryDSL Q-클래스가 생성되어야 합니다.