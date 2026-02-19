---
name: add-or-update-domain
description: Bounded Context에 새로운 도메인을 추가하거나 수정할 때 사용하세요.
---

# Add or Update Domain

## Instructions

새로운 도메인을 추가하거나 기존 도메인을 수정할 때 다음 규칙을 따르세요:

### 1. model 디렉토리

```
├── domain/
│   ├── model/
│   │   ├── command/
│   │   └── query/
```

command 에는 생성/수정/삭제를 위한 조건이나 데이터를 표현하는 클래스가 위치합니다.

command 클래스는 마커 인터페이스로 정의하고, 필요한 속성들을 구현체에 정의합니다. 예를 들어:

```kotlin
sealed interface TaskCommand {
    data class CreateTask(
        val title: TaskTitle,
        val description: TaskDescription?,
        val state: TaskState,
        val step: TaskStep,
        val memberId: MemberId,
        val weekId: WeekId,
    ): TaskCommand

    data class UpdateTask(
        val taskId: TaskId,
        val title: TaskTitle,
        val description: TaskDescription?,
    ) : TaskCommand

    data class DeleteTask(
        val taskId: TaskId,
    ) : TaskCommand
}
```

query 에는 조회를 위한 조건을 표현하는 클래스가 위치합니다.

query 클래스는 반드시 xyz.robinjoon.growweek.common.PageQuery 인터페이스를 구현한 sealed class로 정의하고, 이를 다시 구체적인 클래스가 상속하도록 합니다. 예를 들어:

```kotlin
sealed class TaskQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    object Cursor {
        fun byMemberAndWeek(
            memberId: MemberId,
            weekId: WeekId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): CursorByMemberAndWeek {
            return CursorByMemberAndWeek(
                memberId = memberId,
                weekId = weekId,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byTaskIds(
            taskIds: List<TaskId>,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): CursorByTaskIds {
            return CursorByTaskIds(
                taskIds = taskIds,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    object Offset {
        fun byMemberAndWeek(
            memberId: MemberId,
            weekId: WeekId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): OffsetByMemberAndWeek {
            return OffsetByMemberAndWeek(
                memberId = memberId,
                weekId = weekId,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byTaskIds(
            taskIds: List<TaskId>,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): OffsetByTaskIds {
            return OffsetByTaskIds(
                taskIds = taskIds,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    data class CursorByMemberAndWeek(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: CursorPageInfo
    ) : TaskQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByTaskIds(
        val taskIds: List<TaskId>,
        override val pageInfo: CursorPageInfo
    ) : TaskQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberAndWeek(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: OffsetPageInfo
    ) : TaskQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByTaskIds(
        val taskIds: List<TaskId>,
        override val pageInfo: OffsetPageInfo
    ) : TaskQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
```

그 외 도메인의 엔티티나 VO 등은 model 디렉토리에 함께 위치시키며 다음 조건을 따릅니다.

1. 다른 바운디드 컨텍스트에서 사용해야 하는 것들은 common 에 추가합니다. 단, **추가 위치는 성격에 따라 구분**합니다:

   **Shared Kernel (`common/domain/`)**: 다음 조건을 **모두** 만족하는 경우에만 배치
   - 3개 이상의 BC에서 동일한 의미로 사용
   - 변경 빈도가 매우 낮은 식별자(ID VO) 또는 범용 열거형
   ```kotlin
       @JvmInline
       value class MemberId(val value: Long) {
           init {
               require(value >= 0) { "MemberId must be greater than or equal to 0" }
           }
       }
   ```
   ```
      ├── common/
      │   ├── domain/
      │   │   ├── MemberId.kt      # Shared Kernel (3개+ BC 공유)
   ```

   **BC 간 공유 계약 (`common/contract/{provider-bc}/`)**: 다음 중 하나라도 해당하면 배치
   - 특정 BC의 도메인 개념을 다른 BC가 소비하기 위한 계약 모델
   - BC 간 통신 포트 인터페이스
   - 특정 BC가 발행하는 이벤트 페이로드
   ```
      ├── common/
      │   ├── contract/
      │   │   ├── task/            # task BC가 제공하는 계약
      │   │   │   ├── TaskSummary.kt
      │   │   │   └── TaskSummaryPort.kt
   ```

2. **common 을 제외한 다른 바운디드 컨텍스트의 도메인 모델**은 참조하지 않도록 합니다.

### 2. repository 디렉토리

```
├── domain/
│   ├── model/
│   │   ├── command/
│   │   └── query/
│   ├── repository/
```

repository 디렉토리에는 도메인 모델에 대한 영속성 인터페이스를 정의합니다. 이때, 반드시 saveAll(List<command>) : List<Domain> , findAll(query) : Page<Domain> 메서드만을 포함합니다. 예를 들어:

```kotlin
interface TaskRepository {
    fun saveAll(commands: List<TaskCommand>): List<Task>
    fun findAll(query: TaskQuery): Page<Task>
}
```

### 3. service 디렉토리

```
├── domain/
│   ├── model/
│   │   ├── command/
│   │   └── query/
│   ├── repository/
│   └── service/
```

service 디렉토리는 도메인 서비스가 위치합니다. 단, 도메인 서비스는 [도메인 서비스 추가 필요성 결정 기준 문서](domain-service.md)에 따라 신중하게 설계하고 구현해야 합니다.