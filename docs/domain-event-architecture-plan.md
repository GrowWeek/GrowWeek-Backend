# 도메인 이벤트 아키텍처 계획

## 개요

Bounded Context 간 느슨한 결합을 유지하면서 통신하기 위한 도메인 이벤트 아키텍처를 설계합니다.

### 설계 원칙

1. **common**: 이벤트 인프라 (제네릭 클래스, 인터페이스) + **공유 이벤트 페이로드**
2. **각 BC**: common만 참조, **다른 BC는 절대 참조하지 않음**
3. **이벤트 페이로드**: common에 정의하여 "공유 계약"으로 사용

---

## 패키지 구조

### 1. common 모듈

```
common/
├── domain/
│   ├── TaskId.kt
│   ├── UserId.kt
│   └── RetrospectiveId.kt
├── event/
│   ├── DomainEvent.kt              # 이벤트 래퍼 (제네릭)
│   ├── DomainEventPublisher.kt     # 이벤트 발행 인터페이스
│   ├── DomainEventHandler.kt       # 이벤트 핸들러 인터페이스
│   └── payload/                    # [핵심] 공유 이벤트 페이로드
│       └── RetrospectiveEventPayload.kt
├── config/
│   └── EventConfig.kt              # Bean 설정 (@Configuration)
└── infrastructure/
    └── SpringDomainEventPublisher.kt  # Spring 의존 구현체
```

### 2. 각 Bounded Context

```
{bounded-context}/
├── presentation/
├── application/
│   ├── command/
│   ├── dto/
│   ├── query/
│   ├── service/                 # 이벤트 발행 (common/event 사용)
│   └── usecase/
├── domain/
└── infrastructure/
    ├── persistence/
    ├── external/
    └── event/                   # 이벤트 핸들러 구현체 (common/event 사용)
        └── {EventName}Handler.kt
```

---

## 공통 이벤트 인프라 (common/event)

### DomainEvent.kt

```kotlin
package xyz.robinjoon.growweek.common.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 모든 도메인 이벤트의 기반 인터페이스
 * @param T 이벤트 페이로드 타입
 */
interface DomainEvent<T> {
    val eventId: UUID
    val occurredAt: LocalDateTime
    val payload: T
}

/**
 * 기본 도메인 이벤트 구현
 */
data class DefaultDomainEvent<T>(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val payload: T
) : DomainEvent<T>
```

### DomainEventPublisher.kt

```kotlin
package xyz.robinjoon.growweek.common.event

/**
 * 도메인 이벤트 발행 인터페이스
 */
interface DomainEventPublisher {
    /**
     * 이벤트를 발행합니다.
     * 동기/비동기 여부는 구현체에서 결정합니다.
     */
    fun <T> publish(event: DomainEvent<T>)

    /**
     * 페이로드만으로 이벤트를 발행합니다.
     * DefaultDomainEvent로 래핑됩니다.
     */
    fun <T> publish(payload: T) {
        publish(DefaultDomainEvent(payload = payload))
    }
}
```

### DomainEventHandler.kt

```kotlin
package xyz.robinjoon.growweek.common.event

/**
 * 도메인 이벤트 핸들러 인터페이스
 * @param T 처리할 이벤트 페이로드 타입
 */
interface DomainEventHandler<T> {
    /**
     * 이벤트를 처리합니다.
     */
    fun handle(event: DomainEvent<T>)

    /**
     * 이 핸들러가 처리할 수 있는 이벤트 타입
     */
    fun supports(payloadType: Class<*>): Boolean
}
```

### payload/RetrospectiveEventPayload.kt (공유 이벤트 페이로드)

```kotlin
package xyz.robinjoon.growweek.common.event.payload

import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import java.time.LocalDate

/**
 * Retrospective 관련 이벤트 페이로드 (공유 계약)
 *
 * 발행측(retrospective)과 수신측(task) 모두 이 클래스를 사용합니다.
 * 다른 BC를 직접 참조하지 않고 common의 공유 계약을 통해 통신합니다.
 */
sealed interface RetrospectiveEventPayload {

    /**
     * 회고 완료 시 발행되는 이벤트
     */
    data class Completed(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : RetrospectiveEventPayload

    /**
     * 회고 삭제 시 발행되는 이벤트
     */
    data class Deleted(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveEventPayload
}
```

---

## Bounded Context별 구현

### Retrospective Context (이벤트 발행 측)

#### application/service에서 이벤트 발행

```kotlin
package xyz.robinjoon.growweek.retrospective.application.service

import xyz.robinjoon.growweek.common.event.DomainEventPublisher
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload  // common 참조

@Service
class CompleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val eventPublisher: DomainEventPublisher
) : CompleteRetrospectiveUseCase {

    @Transactional
    override fun execute(command: CompleteRetrospective): RetrospectiveDto {
        // 1. 회고 완료 처리
        val completed = retrospectiveRepository.saveAll(listOf(domainCommand)).first()

        // 2. 이벤트 발행 (common의 공유 페이로드 사용)
        eventPublisher.publish(
            RetrospectiveEventPayload.Completed(
                retrospectiveId = completed.id,
                userId = completed.userId,
                startDate = completed.period.startDate,
                endDate = completed.period.endDate
            )
        )

        return RetrospectiveDto.from(completed)
    }
}
```

### Task Context (이벤트 수신 측)

#### infrastructure/event/RetrospectiveCompletedHandler.kt

```kotlin
package xyz.robinjoon.growweek.task.infrastructure.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload  // common 참조 (다른 BC 참조 X)
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Component
class RetrospectiveCompletedHandler(
    private val taskRepository: TaskRepository
) : DomainEventHandler<RetrospectiveEventPayload.Completed> {

    /**
     * 동기 처리: 같은 트랜잭션 내에서 실행
     * 비동기로 변경하려면 @Async + AFTER_COMMIT으로 변경
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    override fun handle(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        val payload = event.payload

        // 해당 기간의 Task 조회
        val tasks = taskRepository.findAll(
            TaskQuery.OffsetByUserIdAndPeriod(
                userId = payload.userId,
                startDate = payload.startDate,
                endDate = payload.endDate
            )
        ).items

        // 각 Task에 회고 연결
        val linkCommands = tasks.map { task ->
            TaskCommand.LinkRetrospective(
                taskId = task.id,
                retrospectiveId = payload.retrospectiveId
            )
        }

        if (linkCommands.isNotEmpty()) {
            taskRepository.saveAll(linkCommands)
        }
    }

    override fun supports(payloadType: Class<*>): Boolean {
        return payloadType == RetrospectiveEventPayload.Completed::class.java
    }
}
```

---

## 이벤트 발행자 구현체

### common/config/EventConfig.kt (Bean 설정)

```kotlin
package xyz.robinjoon.growweek.common.config

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.robinjoon.growweek.common.event.DomainEventPublisher
import xyz.robinjoon.growweek.common.infrastructure.SpringDomainEventPublisher

@Configuration
class EventConfig {

    @Bean
    fun domainEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher
    ): DomainEventPublisher {
        return SpringDomainEventPublisher(applicationEventPublisher)
    }
}
```

### common/infrastructure/SpringDomainEventPublisher.kt (구현체)

```kotlin
package xyz.robinjoon.growweek.common.infrastructure

import org.springframework.context.ApplicationEventPublisher
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventPublisher

/**
 * Spring ApplicationEventPublisher를 사용한 구현체
 *
 * 위치: common/infrastructure (Spring 의존적인 구현체)
 */
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {

    override fun <T> publish(event: DomainEvent<T>) {
        applicationEventPublisher.publishEvent(event)
    }
}
```

### 구현체 위치 정리

| 파일 | 위치 | 역할 |
|------|------|------|
| DomainEvent.kt | common/event/ | 이벤트 인터페이스 |
| DomainEventPublisher.kt | common/event/ | 발행자 인터페이스 |
| DomainEventHandler.kt | common/event/ | 핸들러 인터페이스 |
| RetrospectiveEventPayload.kt | common/event/payload/ | 공유 이벤트 페이로드 |
| EventConfig.kt | common/config/ | Bean 설정 (@Configuration) |
| SpringDomainEventPublisher.kt | common/infrastructure/ | Spring 의존 구현체 |

---

## 참조 규칙

### 핵심 원칙: 다른 BC는 절대 참조하지 않음

**모든 BC는 common만 참조하고, 다른 BC를 직접 참조하지 않습니다.**

이벤트 통신도 마찬가지로, 공유 이벤트 페이로드를 common에 정의하여 "공유 계약"으로 사용합니다.

### 레이어별 참조 가능 범위

| 레이어 | 참조 가능 | 참조 불가 |
|--------|----------|----------|
| **presentation** | application, common | domain, infrastructure, **다른 BC** |
| **application** | domain, common | infrastructure, **다른 BC** |
| **domain** | common | application, infrastructure, **다른 BC** |
| **infrastructure** | domain, application, common | presentation, **다른 BC** |

### 이벤트 통신 구조

```
┌───────────────────────────────────────────────────────────────────────┐
│                              common                                    │
│  ┌─────────────┐  ┌───────────────────────────────────────────────┐   │
│  │   domain/   │  │                  event/                       │   │
│  │  - TaskId   │  │  - DomainEvent<T>                             │   │
│  │  - UserId   │  │  - DomainEventPublisher (인터페이스)           │   │
│  │  - ...      │  │  - DomainEventHandler<T>                      │   │
│  │             │  │  - payload/                                   │   │
│  │             │  │      └── RetrospectiveEventPayload (공유 계약) │   │
│  └─────────────┘  └───────────────────────────────────────────────┘   │
│  ┌─────────────┐  ┌───────────────────────────────────────────────┐   │
│  │   config/   │  │              infrastructure/                  │   │
│  │  - Event    │  │  - SpringDomainEventPublisher.kt (구현체)     │   │
│  │    Config   │  │                                               │   │
│  └─────────────┘  └───────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ 참조 (common만!)
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  retrospective  │  │      task       │  │    (other BC)   │
│                 │  │                 │  │                 │
│  application/   │  │  infrastructure/│  │                 │
│    service/     │  │    event/       │  │                 │
│  - 이벤트 발행  │  │  - 이벤트 수신  │  │                 │
│                 │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │
         │                    │
         └────────────────────┘
                  ✗ 서로 참조 금지
```

### 참조 규칙 요약

| 소스 위치 | 대상 | 허용 여부 |
|----------|------|----------|
| retrospective/* | common/* | **허용** |
| task/* | common/* | **허용** |
| retrospective/* | task/* | **금지** |
| task/* | retrospective/* | **금지** |

### 올바른 import 예시

```kotlin
// retrospective/application/service/CompleteRetrospectiveService.kt

// 허용: common 참조
import xyz.robinjoon.growweek.common.event.DomainEventPublisher
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload

// 금지: 다른 BC 참조
// import xyz.robinjoon.growweek.task.domain.repository.TaskRepository  // ✗
```

```kotlin
// task/infrastructure/event/RetrospectiveCompletedHandler.kt

// 허용: common 참조
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload

// 금지: 다른 BC 참조
// import xyz.robinjoon.growweek.retrospective.domain.model.Retrospective  // ✗
// import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto  // ✗
```

---

## 동기/비동기 선택 가이드

### 동기 처리 (BEFORE_COMMIT)

```kotlin
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
fun handle(event: DomainEvent<...>) { ... }
```

**사용 시점**:
- 데이터 일관성이 중요한 경우
- 실패 시 전체 트랜잭션 롤백이 필요한 경우
- 현재 케이스 (Task-Retrospective 연결)

### 비동기 처리 (AFTER_COMMIT + @Async)

```kotlin
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handle(event: DomainEvent<...>) { ... }
```

**사용 시점**:
- Eventual Consistency 허용 가능한 경우
- 알림 발송, 로그 기록 등 부가 작업
- 성능이 중요한 경우

---

## 구현 체크리스트

### Phase 1: 공통 인프라 구축 (common)

- [ ] `common/event/DomainEvent.kt` 생성
- [ ] `common/event/DomainEventPublisher.kt` 생성
- [ ] `common/event/DomainEventHandler.kt` 생성
- [ ] `common/event/payload/RetrospectiveEventPayload.kt` 생성 (공유 이벤트 페이로드)
- [ ] `common/infrastructure/SpringDomainEventPublisher.kt` 생성 (Spring 구현체)
- [ ] `common/config/EventConfig.kt` 생성 (Bean 설정)

### Phase 2: Retrospective 컨텍스트 (발행 측)

- [ ] `CompleteRetrospectiveService`에 `DomainEventPublisher` 주입
- [ ] 회고 완료 시 `RetrospectiveEventPayload.Completed` 이벤트 발행

### Phase 3: Task 컨텍스트 (수신 측)

- [ ] `task/infrastructure/event/` 디렉토리 생성
- [ ] `RetrospectiveCompletedHandler.kt` 생성
- [ ] `TaskQuery.OffsetByUserIdAndPeriod` 추가 (필요 시)
- [ ] `TaskRepository.findAll()` 기간 조회 지원 확인

### Phase 4: 테스트

- [ ] 이벤트 발행 단위 테스트
- [ ] 이벤트 핸들러 단위 테스트
- [ ] 통합 테스트 (회고 완료 → Task 연결)

---

## 향후 확장 고려사항

### 1. 이벤트 저장소 (Event Store)

추후 이벤트 소싱이 필요한 경우 이벤트를 DB에 저장하는 구조로 확장 가능

### 2. 메시지 브로커 연동

마이크로서비스 전환 시 Kafka, RabbitMQ 등으로 `DomainEventPublisher` 구현체 교체

### 3. 이벤트 버전 관리

이벤트 스키마 변경 시 버전 관리 전략 필요
