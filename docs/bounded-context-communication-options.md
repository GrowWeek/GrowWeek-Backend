# Bounded Context 간 통신 아키텍처 옵션

## 배경

Task와 Retrospective는 서로 다른 Bounded Context로 분리되어 있습니다.
회고 완료 시 Task에 `retrospectiveId`를 연결해야 하는데, 이는 **두 컨텍스트 간 통신**이 필요한 상황입니다.

```
┌─────────────────┐         ┌─────────────────────┐
│      Task       │ ◄────── │    Retrospective    │
│  (retrospective │         │                     │
│      Id)        │         │                     │
└─────────────────┘         └─────────────────────┘
```

---

## 옵션 비교

| 옵션 | 결합도 | 복잡도 | 트랜잭션 | 적합한 규모 |
|------|--------|--------|----------|-------------|
| 1. 직접 의존성 주입 | 높음 | 낮음 | 단일 | 소규모 |
| 2. 도메인 이벤트 (동기) | 중간 | 중간 | 단일 | 중소규모 |
| 3. 도메인 이벤트 (비동기) | 낮음 | 높음 | 분리 | 대규모 |
| 4. Application Service 조율 | 중간 | 낮음 | 단일 | 중소규모 |
| 5. ACL + Port/Adapter | 낮음 | 중간 | 단일 | 중규모 |

---

## 옵션 1: 직접 의존성 주입

Retrospective 모듈에서 Task의 Repository를 직접 주입받아 사용

### 구현 예시

```kotlin
// CompleteRetrospectiveService.kt
@Service
class CompleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val taskRepository: TaskRepository  // Task 컨텍스트 직접 의존
) : CompleteRetrospectiveUseCase {

    @Transactional
    override fun execute(command: CompleteRetrospective): RetrospectiveDto {
        // 1. 회고 완료
        val completed = retrospectiveRepository.saveAll(...)

        // 2. Task 연결 (직접 호출)
        val tasks = taskRepository.findByPeriod(...)
        val linkCommands = tasks.map { TaskCommand.LinkRetrospective(it.id, completed.id) }
        taskRepository.saveAll(linkCommands)

        return RetrospectiveDto.from(completed)
    }
}
```

### 장점
- 구현이 단순함
- 단일 트랜잭션 보장
- 디버깅 용이

### 단점
- Retrospective → Task 강한 결합
- 컨텍스트 경계 위반
- 순환 의존성 위험

### 적합한 경우
- 프로젝트 초기 단계
- 빠른 구현이 필요할 때
- 팀 규모가 작을 때

---

## 옵션 2: 도메인 이벤트 (동기)

Spring의 `ApplicationEventPublisher`를 활용한 동기 이벤트 발행

### 구현 예시

```kotlin
// Retrospective 컨텍스트 - 이벤트 발행
@Service
class CompleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    override fun execute(command: CompleteRetrospective): RetrospectiveDto {
        val completed = retrospectiveRepository.saveAll(...)

        // 이벤트 발행
        eventPublisher.publishEvent(
            RetrospectiveCompletedEvent(
                retrospectiveId = completed.id,
                userId = completed.userId,
                startDate = completed.period.startDate,
                endDate = completed.period.endDate
            )
        )

        return RetrospectiveDto.from(completed)
    }
}

// 공통 이벤트 정의 (common 모듈)
data class RetrospectiveCompletedEvent(
    val retrospectiveId: RetrospectiveId,
    val userId: UserId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// Task 컨텍스트 - 이벤트 수신
@Component
class RetrospectiveCompletedEventHandler(
    private val taskRepository: TaskRepository
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: RetrospectiveCompletedEvent) {
        val tasks = taskRepository.findByUserIdAndPeriod(
            event.userId, event.startDate, event.endDate
        )
        val commands = tasks.map {
            TaskCommand.LinkRetrospective(it.id, event.retrospectiveId)
        }
        taskRepository.saveAll(commands)
    }
}
```

### 장점
- 컨텍스트 간 느슨한 결합
- 단일 트랜잭션 유지 가능 (`BEFORE_COMMIT`)
- 확장성 좋음 (다른 리스너 추가 용이)

### 단점
- 이벤트 정의 필요
- 디버깅 시 흐름 추적 어려움
- 동기 실행으로 인한 지연 가능

### 적합한 경우
- 중간 규모 프로젝트
- 컨텍스트 분리를 유지하면서 트랜잭션 일관성 필요

---

## 옵션 3: 도메인 이벤트 (비동기)

비동기 이벤트 발행 + Eventual Consistency

### 구현 예시

```kotlin
// Task 컨텍스트 - 비동기 이벤트 수신
@Component
class RetrospectiveCompletedEventHandler(
    private val taskRepository: TaskRepository
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: RetrospectiveCompletedEvent) {
        // 별도 트랜잭션에서 실행
        val tasks = taskRepository.findByUserIdAndPeriod(...)
        val commands = tasks.map { TaskCommand.LinkRetrospective(...) }
        taskRepository.saveAll(commands)
    }
}
```

### 장점
- 완전한 컨텍스트 분리
- 성능 향상 (비동기 처리)
- 마이크로서비스 전환 용이

### 단점
- Eventual Consistency (일시적 불일치)
- 실패 시 재처리 로직 필요
- 복잡도 증가

### 적합한 경우
- 대규모 시스템
- 마이크로서비스 지향
- 즉각적인 일관성이 필수가 아닐 때

---

## 옵션 4: Application Service 조율 (Orchestrator)

상위 레이어에서 두 컨텍스트를 조율하는 별도 서비스

### 구현 예시

```kotlin
// common 또는 별도 orchestration 모듈
@Service
class RetrospectiveWorkflowService(
    private val completeRetrospectiveUseCase: CompleteRetrospectiveUseCase,
    private val linkTasksToRetrospectiveUseCase: LinkTasksToRetrospectiveUseCase
) {
    @Transactional
    fun completeRetrospectiveWithTaskLinking(command: CompleteRetrospectiveCommand): RetrospectiveDto {
        // 1. 회고 완료
        val retrospective = completeRetrospectiveUseCase.execute(command.toRetrospectiveCommand())

        // 2. Task 연결
        linkTasksToRetrospectiveUseCase.execute(
            LinkTasksCommand(
                retrospectiveId = retrospective.id,
                userId = command.userId,
                startDate = retrospective.period.startDate,
                endDate = retrospective.period.endDate
            )
        )

        return retrospective
    }
}
```

### 장점
- 각 컨텍스트는 독립적 유지
- 워크플로우 로직 명확
- 트랜잭션 경계 명확

### 단점
- 추가 레이어 필요
- Presentation 레이어에서 호출 대상 변경 필요

### 적합한 경우
- 복잡한 워크플로우가 있을 때
- 여러 컨텍스트 조율이 필요할 때

---

## 옵션 5: ACL + Port/Adapter

Anti-Corruption Layer를 통한 컨텍스트 보호

### 구현 예시

```kotlin
// Retrospective 컨텍스트 내 Port 정의
interface TaskLinkingPort {
    fun linkTasksToRetrospective(
        retrospectiveId: RetrospectiveId,
        userId: UserId,
        period: RetrospectivePeriod
    )
}

// Retrospective 컨텍스트 - 서비스
@Service
class CompleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val taskLinkingPort: TaskLinkingPort  // Port 의존
) {
    @Transactional
    override fun execute(command: CompleteRetrospective): RetrospectiveDto {
        val completed = retrospectiveRepository.saveAll(...)

        taskLinkingPort.linkTasksToRetrospective(
            completed.id, completed.userId, completed.period
        )

        return RetrospectiveDto.from(completed)
    }
}

// Infrastructure 레이어 - Adapter 구현
@Component
class TaskLinkingAdapter(
    private val taskRepository: TaskRepository
) : TaskLinkingPort {
    override fun linkTasksToRetrospective(
        retrospectiveId: RetrospectiveId,
        userId: UserId,
        period: RetrospectivePeriod
    ) {
        val tasks = taskRepository.findByUserIdAndPeriod(userId, period.startDate, period.endDate)
        val commands = tasks.map { TaskCommand.LinkRetrospective(it.id, retrospectiveId) }
        taskRepository.saveAll(commands)
    }
}
```

### 장점
- 컨텍스트 독립성 유지
- 인터페이스를 통한 추상화
- 테스트 용이 (Port Mock 가능)
- DDD 원칙 준수

### 단점
- 코드량 증가
- 초기 설계 비용

### 적합한 경우
- DDD를 엄격하게 적용하는 프로젝트
- 장기적인 유지보수 고려

---

## 권장 사항

### 현재 프로젝트 규모 및 상황 고려

| 요소 | 현재 상태 |
|------|----------|
| 프로젝트 규모 | 중소규모 |
| 팀 규모 | 소규모 추정 |
| 아키텍처 | 모놀리식 |
| 트랜잭션 요구 | 즉각적 일관성 필요 |

### 추천 순위

1. **옵션 2: 도메인 이벤트 (동기)** - 권장
   - 적절한 결합도와 복잡도 균형
   - 트랜잭션 일관성 보장
   - 향후 확장 용이

2. **옵션 5: ACL + Port/Adapter** - 장기적 관점
   - DDD 원칙 준수
   - 테스트 용이
   - 초기 투자 필요

3. **옵션 1: 직접 의존성** - 빠른 해결 필요 시
   - 가장 단순한 구현
   - 추후 리팩토링 필요

---

## 결론

현재 상황에서는 **옵션 2 (도메인 이벤트 동기)** 또는 **옵션 5 (ACL + Port/Adapter)**를 권장합니다.

- 빠른 구현이 우선이라면: **옵션 1** → 추후 리팩토링
- 균형 잡힌 접근: **옵션 2**
- 장기적 아키텍처 고려: **옵션 5**
