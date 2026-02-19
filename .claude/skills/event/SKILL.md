---
name: add-or-update-event-handler
description: Bounded Context에 새로운 도메인 이벤트 핸들러를 추가하거나 수정할 때 사용하세요.
---

# Add or Update Event Handler

## Instructions

도메인 이벤트 핸들러를 추가하거나 수정할 때 다음 규칙을 따르세요:

### 1. event 디렉토리 위치

이벤트 핸들러는 이벤트를 **수신하는 쪽** BC의 `infrastructure/event/` 디렉토리에 위치합니다. 기능상 필요하지 않다면 이 디렉토리 안에는 파일이 존재하지 않을 수 있습니다.

```
{bounded-context}/
└── infrastructure/
    └── event/
        └── SomeEventHandler.kt
```

### 2. 이벤트 시스템 구조

이벤트 관련 공통 인터페이스와 구현체는 `common/` 레이어에 위치합니다:

- `common/event/DomainEvent.kt` — 이벤트 래퍼 인터페이스 (Shared Kernel)
- `common/event/DomainEventHandler.kt` — 핸들러 인터페이스 (Shared Kernel)
- `common/event/DomainEventPublisher.kt` — 발행 인터페이스 (Shared Kernel)
- `common/contract/{publisher-bc}/` — 이벤트 페이로드 정의 (BC 간 공유 계약)
- `common/infrastructure/DomainEventDispatcher.kt` — 중앙 디스패처 (Spring `@TransactionalEventListener` 기반)

### 3. 핸들러 작성 규칙

핸들러는 `DomainEventHandler<T>` 인터페이스를 구현하고, `@Component`로 등록합니다. `DomainEventDispatcher`가 자동으로 핸들러를 수집하여 이벤트를 분배합니다.

```kotlin
@Component
class SomeEventHandler(
    private val someRepository: SomeRepository,
) : DomainEventHandler<SomeEventPayload.Completed> {

    override fun handle(event: DomainEvent<SomeEventPayload.Completed>) {
        val payload = event.payload
        // 이벤트 처리 로직
    }

    override fun supports(payloadType: KClass<*>): Boolean =
        payloadType == SomeEventPayload.Completed::class
}
```

### 4. 페이로드 작성 규칙

이벤트 페이로드는 **발행하는 BC가 제공하는 계약**이므로 `common/contract/{publisher-bc}/` 디렉토리에 sealed interface로 정의합니다. 페이로드 필드에는 `common/domain/`의 VO 클래스를 사용합니다.

```
common/
└── contract/
    └── {publisher-bc}/          # 이벤트를 발행하는 BC 이름
        └── SomeEventPayload.kt
```

```kotlin
sealed interface SomeEventPayload {
    data class Completed(
        val someId: SomeId,
        val memberId: MemberId,
    ) : SomeEventPayload
}
```

### 5. 이벤트 발행

이벤트를 발행하는 쪽(주로 application service)에서는 `DomainEventPublisher`를 주입받아 사용합니다.

```kotlin
@Service
class SomeService(
    private val eventPublisher: DomainEventPublisher,
) {
    fun doSomething() {
        // 비즈니스 로직 수행
        eventPublisher.publish(SomeEventPayload.Completed(...))
    }
}
```

### 6. 주의사항

- 이벤트 핸들러는 **수신하는 BC의 infrastructure**에 위치합니다. 발행하는 BC에 두지 않습니다.
- 핸들러 내부에서는 자신의 BC의 domain repository/service만 사용합니다.
- `DomainEventDispatcher`는 `TransactionPhase.BEFORE_COMMIT` 단계에서 실행되므로, 핸들러의 작업은 발행자와 같은 트랜잭션 내에서 수행됩니다.
