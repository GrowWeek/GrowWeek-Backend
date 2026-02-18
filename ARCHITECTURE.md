# GrowWeek Backend Architecture

## 개요

GrowWeek Backend는 **DDD (Domain-Driven Design)**, **Clean Architecture**, **CQRS (Command Query Responsibility Segregation)** 패턴을 기반으로 설계된 Spring Boot 애플리케이션입니다.

## 기술 스택

### Backend Framework
- **Spring Boot 4.0.0** (Spring Framework 7.x)
- **Kotlin 2.2.21**
- **Java 24**

### Data Access
- **Exposed ORM 1.0.0-rc-4** (Kotlin DSL ORM)
- **Spring Data JDBC** (경량 데이터 접근)
- **PostgreSQL** (메인 데이터베이스)
- **H2 Database** (개발/테스트)
- **Spring Data Redis** (캐싱, 세션 저장소)

### Security
- **Spring Security 7.x**
- **JJWT 0.12.6** (JWT 토큰 처리)

### External Integration
- **Spring Cloud OpenFeign** (외부 API 호출)

### Documentation
- **SpringDoc OpenAPI 2.8.4** (Swagger UI)

### Testing
- **Kotest 6.0.0** (Kotlin 테스트 프레임워크)
- **MockK 1.14.6** (Kotlin 모킹 라이브러리)
- **JaCoCo** (코드 커버리지, 최소 70%)

## 아키텍처 원칙

### 1. Domain-Driven Design (DDD)
- Bounded Context별로 도메인을 분리
- 도메인 로직은 Domain Layer에 집중
- 풍부한 도메인 모델(Rich Domain Model) 지향

### 2. Clean Architecture
- 의존성 방향: Presentation → Application → Domain ← Infrastructure
- Domain Layer는 외부 의존성이 없는 순수한 비즈니스 로직
- Infrastructure Layer가 Domain의 인터페이스를 구현

### 3. CQRS
- Command와 Query의 명확한 분리
- Command: 상태 변경 작업
- Query: 데이터 조회 작업

## 주요 기술 선택 이유

### Exposed ORM
- **Kotlin DSL 기반**: 타입 안전한 쿼리 작성
- **경량**: JPA보다 가볍고 명시적인 제어 가능
- **유연성**: SQL에 가까운 직관적인 쿼리 작성

### Spring Data JDBC
- **단순성**: JPA의 복잡한 영속성 관리 없이 경량 데이터 접근
- **명시적 제어**: 영속성 컨텍스트 없이 명확한 쿼리 실행

### Redis
- **캐싱**: 조회 성능 최적화
- **세션 관리**: 분산 환경에서의 세션 저장소
- **임시 데이터**: TTL 기반 데이터 관리

### PostgreSQL
- **안정성**: 엔터프라이즈급 RDBMS
- **확장성**: JSON 타입, Full-text search 등 다양한 기능
- **트랜잭션**: ACID 보장

## 디렉토리 구조

```
xyz.robinjoon.growweek/
├── ServerApplication.kt          # Spring Boot 애플리케이션 진입점
├── common/                        # 공통 모듈
└── {bounded-context}/            # 도메인별 Bounded Context (예: task)
    ├── presentation/             # API Layer (Controllers, DTOs)
    │   └── rest/                # REST API
    │       ├── request/         # 요청 DTO
    │       ├── response/        # 응답 DTO
    │       └── controller/      # REST Controllers
    ├── application/             # Application Layer (Use Cases)
    │   ├── command/            # Command 표현 DTO
    │   ├── dto/                # 응답 DTO
    │   ├── query/              # Query 표현 DTO
    │   ├── service/            # Application Services (구현체)
    │   └── usecase/            # Use Case 인터페이스
    ├── domain/                  # Domain Layer (비즈니스 로직)
    │   ├── model/              # 도메인 모델
    │   │   ├── command/       # Command 모델
    │   │   └── query/         # Query 모델
    │   ├── repository/         # Repository 인터페이스
    │   └── service/            # Domain Services
    └── infrastructure/          # Infrastructure Layer (구현체)
        ├── persistence/        # 영속성 구현 (JPA, Repository Impl)
        ├── event/              # 도메인 이벤트 핸들러
        └── external/           # 외부 시스템 연동
```

## 계층별 상세 설명

### Presentation Layer (presentation/)
- **역할**: HTTP 요청/응답 처리, API 엔드포인트 정의
- **포함 요소**:
  - **rest/controller/**: REST Controllers
  - **rest/request/**: 클라이언트 요청 데이터 구조 (Primitive Type 사용)
  - **rest/response/**: 클라이언트 응답 데이터 구조 (Primitive Type 사용)
- **의존성**: Application Layer에만 의존
- **주의사항**: 도메인 레이어의 VO 클래스를 직접 사용하지 않고, Primitive Type 또는 데이터 클래스 형태의 DTO 사용

### Application Layer (application/)
- **역할**: Use Case 구현, 트랜잭션 관리, 도메인 객체 조율
- **포함 요소**:
  - **command/**: 상태를 변경하는 Command DTO (생성, 수정, 삭제)
  - **dto/**: 서비스/유스케이스에서 반환하는 응답 DTO
  - **query/**: 데이터를 조회하는 Query DTO (PageQuery 인터페이스 구현)
  - **usecase/**: Use Case 인터페이스 정의
  - **service/**: Application Service (Use Case 구현체)
- **의존성**: Domain Layer에 의존
- **주의사항**: command/query/dto 내부 필드에서 common 등 다른 바운디드 컨텍스트의 도메인 VO 클래스 사용 가능

### Domain Layer (domain/)
- **역할**: 핵심 비즈니스 로직, 비즈니스 규칙
- **포함 요소**:
  - **model/**: 도메인 엔티티, Value Objects, Aggregates
    - **command/**: 쓰기 작업에 최적화된 모델
    - **query/**: 읽기 작업에 최적화된 모델
  - **repository/**: Repository 인터페이스 (구현체는 Infrastructure)
  - **service/**: Domain Service (엔티티/VO로 표현하기 어려운 비즈니스 로직)
- **의존성**: 외부 의존성 없음 (순수 Kotlin/Java)

### Infrastructure Layer (infrastructure/)
- **역할**: 기술적 구현, 외부 시스템 연동
- **포함 요소**:
  - **persistence/**:
    - Exposed ORM Table 정의
    - Repository 구현체 (Exposed DSL 쿼리)
    - Redis Repository (캐싱)
    - 데이터베이스 매핑 로직
  - **event/**:
    - 다른 BC에서 발행한 도메인 이벤트를 수신하는 핸들러
    - `DomainEventHandler` 인터페이스 구현체
  - **external/**:
    - OpenFeign 클라이언트 (외부 API 호출)
    - 외부 서비스 어댑터
- **의존성**: Domain Layer의 인터페이스를 구현

### Common Layer (common/)
- **역할**: 모든 Bounded Context에서 공유하는 공통 요소
- **포함 요소**:
  - 공통 예외 및 에러 핸들러
  - 공통 유틸리티
  - 공통 Value Objects
  - 기반 인터페이스/추상 클래스
  - 공통 설정 (Security, OpenAPI, Redis 등)
  - 공통 응답 포맷
- **하위 구조**: 다른 Bounded Context의 레이어 구조(presentation, application, domain, infrastructure)에서 필요한 것만 만들어서 사용합니다.

## CQRS 패턴 적용

### Command Side
```
Request DTO → Controller → Command UseCase → Domain Model (command/)
  → Repository (Exposed ORM) → PostgreSQL
```
- 상태 변경에 집중
- 비즈니스 로직 수행
- 트랜잭션 관리 (@Transactional)
- Exposed DSL을 통한 쓰기 작업

### Query Side
```
Request → Controller → Query UseCase → Domain Model (query/)
  → Repository (Exposed DSL + Redis) → PostgreSQL / Redis
```
- 데이터 조회에 최적화
- 읽기 전용
- Redis 캐싱을 통한 성능 최적화
- Exposed DSL 조인 쿼리 최적화
- 필요시 별도 Read Model 구성 가능

## 새로운 Bounded Context 추가 시 구조

새로운 도메인(예: `user`)을 추가할 때는 다음 구조를 따릅니다:

```
user/
├── presentation/
│   └── rest/
│       ├── request/
│       ├── response/
│       └── controller/
├── application/
│   ├── command/
│   ├── dto/
│   ├── query/
│   ├── service/
│   └── usecase/
├── domain/
│   ├── model/
│   │   ├── command/
│   │   └── query/
│   ├── repository/
│   └── service/
└── infrastructure/
    ├── persistence/
    └── external/
```

---

## Claude Skills 스켈레톤

프로젝트의 일관성을 유지하기 위해 다음 Claude Skills를 정의할 수 있습니다:

### 1. add-bounded-context
**목적**: 새로운 Bounded Context(도메인) 추가

**내용**:
- 표준 디렉토리 구조 생성
- 기본 패키지 구조 설정
- ...

### 2. add-domain-model
**목적**: 도메인 모델 생성

**내용**:
- Entity vs Value Object 판단 기준
- Command/Query 모델 분리 기준
- Aggregate Root 식별 방법
- ...

### 3. add-domain-service
**목적**: 도메인 서비스 추가

**내용**:
- Domain Service 추가 기준
- 언제 Entity/VO 대신 Domain Service를 사용하는가
- Domain Service vs Application Service 구분
- ...

### 4. add-use-case
**목적**: Application Use Case 추가

**내용**:
- Command Use Case 작성 패턴
- Query Use Case 작성 패턴
- 트랜잭션 처리 방법
- ...

### 5. add-repository
**목적**: Repository 인터페이스 및 구현체 추가

**내용**:
- Repository 인터페이스 정의 위치 (domain/repository/)
- Repository 구현체 위치 (infrastructure/persistence/)
- Exposed ORM Table 정의 방법
- Exposed DSL 쿼리 작성 패턴
- Redis 캐싱 적용 기준
- 메서드 명명 규칙
- ...

### 6. add-api-endpoint
**목적**: REST API 엔드포인트 추가

**내용**:
- Controller 작성 규칙
- Request/Response DTO 작성 규칙
- OpenAPI 애노테이션 작성 (@Operation, @Schema 등)
- API 명명 규칙
- 에러 응답 처리
- ...

### 7. add-value-object
**목적**: Value Object 추가

**내용**:
- Value Object 작성 기준
- 불변성 보장 방법
- 유효성 검증 위치
- ...

### 8. add-aggregate
**목적**: Aggregate 추가

**내용**:
- Aggregate 경계 설정 기준
- Aggregate Root 식별
- 일관성 경계 정의
- ...

### 9. implement-cqrs-command
**목적**: CQRS Command 구현

**내용**:
- Command Model 작성
- Command Handler 작성
- 상태 변경 로직 구현
- ...

### 10. implement-cqrs-query
**목적**: CQRS Query 구현

**내용**:
- Query Model 작성
- Query Handler 작성
- 읽기 최적화 전략
- Redis 캐싱 활용
- ...

### 11. add-external-api-client
**목적**: 외부 API 클라이언트 추가 (OpenFeign)

**내용**:
- FeignClient 인터페이스 작성
- Fallback 처리
- 에러 핸들링
- infrastructure/external/ 위치
- ...

### 12. add-security-config
**목적**: 보안 설정 추가

**내용**:
- Spring Security 설정
- JWT 인증/인가 처리
- 엔드포인트별 권한 설정
- CORS 설정
- ...

### 13. add-test-case
**목적**: 테스트 케이스 추가

**내용**:
- Kotest 기반 테스트 작성
- MockK를 활용한 모킹
- 단위 테스트 vs 통합 테스트 구분
- 테스트 커버리지 기준 (70%)
- Given-When-Then 패턴
- ...
