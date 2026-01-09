# 회고 완료 기간 검증 기능 구현

## 1. 문제 정의

### 1.1 발견된 버그

회고가 완료된 주차에 할일을 추가하거나 수정할 수 있는 버그가 발견되었습니다.

**기대 동작:**
- 회고가 완료된 기간에는 할일을 추가할 수 없어야 함
- 회고가 완료된 기간의 할일은 수정할 수 없어야 함

**실제 동작:**
- 회고 완료 여부와 관계없이 할일 추가/수정이 가능했음

### 1.2 근본 원인 분석

버그의 원인은 두 가지였습니다:

#### 원인 1: CreateTaskService의 검증 로직 부재

`CreateTaskService`에서 할일 생성 시 해당 기간에 완료된 회고가 있는지 검증하지 않았습니다.

```kotlin
// 기존 코드 - 검증 없이 바로 저장
override fun execute(command: TaskApplicationCommand.CreateTask): TaskDto {
    val domainCommand = TaskCommand.CreateTask(...)
    val savedTasks = taskRepository.saveAll(listOf(domainCommand))
    return TaskDto.from(savedTasks.first())
}
```

#### 원인 2: ExposedTaskRepository의 TODO 미구현

`ExposedTaskRepository`에서 Task 수정 시 `retrospectiveDate`를 조회하는 로직이 TODO로 남아있어 항상 `null`을 반환했습니다. 이로 인해 도메인 모델(`Task.kt`)의 `validateModification()` 검증이 우회되었습니다.

```kotlin
// 기존 코드 - TODO로 인해 항상 null 반환
val retrospectiveDate = existingTask.retrospectiveId?.let { retroId ->
    // TODO: CompletedRetrospectivePeriodRepository를 사용하여 조회
    null
}
```

## 2. 해결 방안

### 2.1 아키텍처 제약 사항

프로젝트는 DDD(Domain-Driven Design)와 Clean Architecture를 따르며, 다음과 같은 제약이 있습니다:

- **Bounded Context 분리**: Task BC에서 Retrospective BC를 직접 참조할 수 없음
- **도메인 이벤트 활용**: BC 간 통신은 도메인 이벤트를 통해 이루어짐

### 2.2 해결 컨셉: 이벤트 기반 데이터 동기화

```
┌─────────────────────────────────────────────────────────────────┐
│                      Retrospective BC                            │
│                                                                  │
│  ┌──────────────────────┐                                       │
│  │ CompleteRetrospective│                                       │
│  │      Service         │                                       │
│  └──────────┬───────────┘                                       │
│             │                                                    │
│             │ 회고 완료                                           │
│             ▼                                                    │
│  ┌──────────────────────┐                                       │
│  │   Domain Event       │                                       │
│  │ RetrospectiveCompleted│─────────────────────────────────────┐│
│  └──────────────────────┘                                      ││
└────────────────────────────────────────────────────────────────┘│
                                                                   │
┌──────────────────────────────────────────────────────────────────┼┐
│                         Task BC                                  ││
│                                                                  ││
│  ┌──────────────────────┐      ┌───────────────────────────┐   ││
│  │ RetrospectiveCompleted│◄─────│     Domain Event Bus      │◄──┘│
│  │      Handler          │      └───────────────────────────┘    │
│  └──────────┬───────────┘                                        │
│             │                                                     │
│             │ 1. 완료된 회고 기간 저장                              │
│             │ 2. Task에 retrospectiveId 연결                      │
│             ▼                                                     │
│  ┌──────────────────────┐      ┌───────────────────────────┐    │
│  │ CompletedRetrospective│      │      CreateTaskService    │    │
│  │   PeriodRepository    │◄─────│                           │    │
│  └──────────────────────┘      └───────────────────────────┘    │
│             │                              │                      │
│             │                              │ 검증: 완료된 회고      │
│             │                              │ 기간과 겹치는지 확인   │
│             ▼                              ▼                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              CompletedRetrospectivePeriod                 │   │
│  │  - retrospectiveId                                        │   │
│  │  - memberId                                               │   │
│  │  - startDate                                              │   │
│  │  - endDate                                                │   │
│  │  - completedAt                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

**핵심 아이디어:**

회고가 완료되면 도메인 이벤트를 통해 Task BC에 해당 정보를 전달하고, Task BC는 자체적으로 `CompletedRetrospectivePeriod`를 저장하여 할일 생성/수정 시 검증에 사용합니다.

이 방식의 장점:
- BC 간 직접 의존성 없음
- Task BC가 검증에 필요한 데이터를 자체적으로 보유
- 이벤트 기반의 느슨한 결합(Loose Coupling)

## 3. 구현 상세

### 3.1 도메인 모델 추가

**CompletedRetrospectivePeriod** - 완료된 회고 기간 정보

```kotlin
data class CompletedRetrospectivePeriod(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completedAt: LocalDateTime,
) {
    // 특정 기간과 겹치는지 확인
    fun overlaps(periodStart: LocalDate, periodEnd: LocalDate): Boolean =
        !periodEnd.isBefore(startDate) && !periodStart.isAfter(endDate)

    // 특정 날짜가 기간 내에 있는지 확인
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)
}
```

### 3.2 이벤트 핸들러 수정

**RetrospectiveCompletedHandler** - 회고 완료 이벤트 처리

```kotlin
@Component
class RetrospectiveCompletedHandler(
    private val taskRepository: TaskRepository,
    private val completedRetrospectivePeriodRepository: CompletedRetrospectivePeriodRepository,
) : DomainEventHandler<RetrospectiveEventPayload.Completed> {

    override fun handle(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        val payload = event.payload

        // 1. 완료된 회고 기간 정보 저장
        saveCompletedRetrospectivePeriod(payload)

        // 2. 해당 기간의 Task들에 retrospectiveId 연결
        linkTasksToRetrospective(payload)
    }
}
```

### 3.3 서비스 레이어 검증 추가

**CreateTaskService** - 할일 생성 시 검증

```kotlin
@Service
class CreateTaskService(
    private val taskRepository: TaskRepository,
    private val completedRetrospectivePeriodRepository: CompletedRetrospectivePeriodRepository,
) : CreateTaskUseCase {

    override fun execute(command: TaskApplicationCommand.CreateTask): TaskDto {
        // 회고 완료된 기간과 겹치는지 검증
        validateNotInCompletedRetrospectivePeriod(command)

        // ... 기존 로직
    }

    private fun validateNotInCompletedRetrospectivePeriod(command: ...) {
        val query = CompletedRetrospectivePeriodQuery.Offset.byMemberIdAndOverlappingPeriod(
            memberId = command.memberId,
            periodStart = command.startDate,
            periodEnd = command.dueDate,
        )

        val overlappingPeriods = completedRetrospectivePeriodRepository.findAll(query).items

        if (overlappingPeriods.isNotEmpty()) {
            val period = overlappingPeriods.first()
            throw IllegalArgumentException(
                "회고가 완료된 기간(${period.startDate} ~ ${period.endDate})에는 할일을 추가할 수 없습니다."
            )
        }
    }
}
```

### 3.4 Repository 수정

**ExposedTaskRepository** - retrospectiveDate 조회 구현

```kotlin
private fun findRetrospectiveEndDate(retrospectiveId: RetrospectiveId): LocalDate? =
    CompletedRetrospectivePeriodTable
        .selectAll()
        .where { CompletedRetrospectivePeriodTable.retrospectiveId eq retrospectiveId.value }
        .singleOrNull()
        ?.get(CompletedRetrospectivePeriodTable.endDate)
```

이제 Task 수정 시 실제 회고 종료일을 조회하여 도메인 모델의 `validateModification()` 검증이 정상 동작합니다.

## 4. 파일 변경 목록

### 4.1 신규 파일

| 파일 경로 | 설명 |
|----------|------|
| `task/domain/model/CompletedRetrospectivePeriod.kt` | 완료된 회고 기간 도메인 모델 |
| `task/domain/model/command/CompletedRetrospectivePeriodCommand.kt` | Command 클래스 (Save, Delete) |
| `task/domain/model/query/CompletedRetrospectivePeriodQuery.kt` | Query 클래스 (기간 겹침 조회 등) |
| `task/domain/repository/CompletedRetrospectivePeriodRepository.kt` | Repository 인터페이스 |
| `task/infrastructure/persistence/CompletedRetrospectivePeriodTable.kt` | Exposed 테이블 정의 |
| `task/infrastructure/persistence/ExposedCompletedRetrospectivePeriodRepository.kt` | Repository 구현체 |

### 4.2 수정 파일

| 파일 경로 | 변경 내용 |
|----------|----------|
| `task/infrastructure/event/RetrospectiveCompletedHandler.kt` | 완료된 회고 기간 저장 로직 추가 |
| `task/application/service/CreateTaskService.kt` | 회고 완료 기간 검증 로직 추가 |
| `task/infrastructure/persistence/ExposedTaskRepository.kt` | retrospectiveDate 조회 TODO 구현 |

### 4.3 테스트 파일

| 파일 경로 | 변경 내용 |
|----------|----------|
| `task/application/service/CreateTaskServiceTest.kt` | 회고 완료 기간 검증 테스트 추가 |
| `task/infrastructure/event/RetrospectiveCompletedHandlerTest.kt` | Repository mock 추가 |
| `common/event/RetrospectiveCompletedIntegrationTest.kt` | Repository mock 추가 |

## 5. 커밋 히스토리

```
8300e1a test: RetrospectiveCompletedHandler 테스트 수정
abd2878 test: CreateTaskService 회고 완료 기간 검증 테스트 추가
5b45ffd fix(task): ExposedTaskRepository TODO 수정 - retrospectiveDate 조회
88f4d06 feat(task): CreateTaskService에 회고 완료 기간 검증 추가
114ba6c feat(task): RetrospectiveCompletedHandler에 완료된 회고 기간 저장 추가
5e93d24 feat(task): CompletedRetrospectivePeriod Infrastructure 추가
310efe4 feat(task): CompletedRetrospectivePeriod 도메인 모델 추가
```

## 6. 검증 방법

### 6.1 단위 테스트

```bash
./gradlew test --tests "xyz.robinjoon.growweek.task.application.service.CreateTaskServiceTest"
```

테스트 시나리오:
- 회고가 완료된 기간에 할일 생성 시 `IllegalArgumentException` 발생
- 회고가 완료되지 않은 기간에는 정상적으로 할일 생성

### 6.2 통합 테스트

```bash
./gradlew test --tests "xyz.robinjoon.growweek.common.event.RetrospectiveCompletedIntegrationTest"
```

테스트 시나리오:
- 회고 완료 시 완료된 기간 정보가 저장됨
- 회고 완료 시 해당 기간의 Task에 retrospectiveId가 연결됨

## 7. 향후 고려사항

### 7.1 UpdateTaskService 검증 추가

현재 `CreateTaskService`에만 검증 로직이 추가되었습니다. `UpdateTaskService`에도 동일한 검증 로직 추가를 고려해야 합니다. (단, 현재는 `Task.validateModification()` 도메인 메서드가 동작하므로 Repository 레벨에서 검증됨)

### 7.2 데이터 정합성

회고 완료 취소(DONE → IN_PROGRESS) 시 `CompletedRetrospectivePeriod` 삭제 로직이 필요할 수 있습니다. 현재 시스템에서 회고 완료 취소가 가능한지 확인 후 구현이 필요합니다.
