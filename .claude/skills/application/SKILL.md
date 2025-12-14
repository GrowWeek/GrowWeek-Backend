---
name: add=or-update-application
description: Bounded Context에 새로운 application 레이어를 추가하거나 수정할 때 사용하세요.
---

# Add or Update Application

## Instructions

새로운 애플리케이션 레이어를 추가하거나 기존 애플리케이션 레이어를 수정할 때 다음 규칙을 따르세요:

### 1. application 디렉토리

```
└── application/
    ├── command/
    ├── dto/
    ├── query/
    ├── service/
    └── usecase/
```

### 2. command 디렉토리

command 에는 생성/수정/삭제를 위한 조건이나 데이터를 표현하는 클래스가 위치합니다.

command 클래스는 마커 인터페이스로 정의하고, 필요한 속성들을 구현체에 정의합니다. 예를 들어:
```kotlin
sealed interface TaskApplicationCommand {
    data class CreateTask(
        val title: TaskTitle,
        val description: TaskDescription?,
        val state: TaskState,
        val step: TaskStep,
        val memberId: MemberId,
        val weekId: WeekId,
    ) : TaskApplicationCommand

    data class UpdateTask(
        val taskId: TaskId,
        val title: TaskTitle,
        val description: TaskDescription?,
    ) : TaskApplicationCommand

    data class DeleteTask(
        val taskId: TaskId,
    ) : TaskApplicationCommand
}
```

이 command 클래스는 domain/model/command 디렉토리의 클래스와 유사하지만, 애플리케이션 레이어에 맞게 조정될 수 있습니다.
또한, 내부 필드에서는 common 과 같은 바운디드 컨텍스트의 domain 레이어의 VO 클래스를 사용할 수 있습니다.

### 3. dto 디렉토리
dto 디렉토리에는 애플리케이션 레이어에서 사용하는 서비스나 유스케이스에서 반환하는 응답 DTO를 정의할 수 있습니다. 예를 들어:

```kotlin
data class TaskDto(
    val id: TaskId,
    val title: TaskTitle,
    val description: TaskDescription?,
    val state: TaskState,
    val step: TaskStep,
    val memberId: MemberId,
    val weekId: WeekId,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```
또한, 내부 필드에서는 common 과 같은 바운디드 컨텍스트의 domain 레이어의 VO 클래스를 사용할 수 있습니다.

### 4. query 디렉토리

query 에는 조회를 위한 조건을 표현하는 클래스가 위치합니다.

query 클래스는 반드시 xyz.robinjoon.growweek.common.PageQuery 인터페이스를 구현한 sealed class로 정의하고, 이를 다시 구체적인 클래스가 상속하도록 합니다. 예를 들어:

```kotlin
sealed class TaskApplicationQuery(
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
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByTaskIds(
        val taskIds: List<TaskId>,
        override val pageInfo: CursorPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberAndWeek(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: OffsetPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByTaskIds(
        val taskIds: List<TaskId>,
        override val pageInfo: OffsetPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
```

이 query 클래스는 domain/model/query 디렉토리의 클래스와 유사하지만, 애플리케이션 레이어에 맞게 조정될 수 있습니다.
또한, 내부 필드에서는 common 과 같은 바운디드 컨텍스트의 domain 레이어의 VO 클래스를 사용할 수 있습니다.

### 5. usecase 및 service 디렉토리

usecase 디렉토리에는 애플리케이션의 유스케이스(Use Case)를 interface로 정의합니다. 각 유스케이스는 command 또는 query를 처리하는 책임을 집니다. 
service 디렉토리에는 usecase 인터페이스의 구현체가 위치합니다. 이 구현체는 도메인 레이어의 리포지토리 (혹은 도메인 서비스)와 상호작용하여 비즈니스 로직을 수행합니다.
예를 들어:

```kotlin
// usecase/CreateTaskUseCase.kt

interface CreateTaskUseCase {
    fun createTask(command: TaskApplicationCommand.CreateTask): TaskDto
}
```

```kotlin
// service/CreateTaskService.kt
class CreateTaskService(
    private val taskRepository: TaskRepository
) : CreateTaskUseCase {
    override fun createTask(command: TaskApplicationCommand.CreateTask): TaskDto {
        val task = Task.create(
            title = command.title,
            description = command.description,
            state = command.state,
            step = command.step,
            memberId = command.memberId,
            weekId = command.weekId
        )
        taskRepository.save(task)
        return task.toDto()
    }
}
```

로직이 복잡할 것이라 예상된다면 구체적인 command/query 별로 usecase/service 를 나누는 것을 허용합니다. 예를 들어:

```kotlin
// usecase/UpdateTaskUseCase.kt
interface UpdateTaskUseCase {
    fun updateTask(command: TaskApplicationCommand.UpdateTask): TaskDto
}
```

```kotlin
// service/UpdateTaskService.kt
class UpdateTaskService(
    private val taskRepository: TaskRepository
) : UpdateTaskUseCase {
    override fun updateTask(command: TaskApplicationCommand.UpdateTask): TaskDto {
        val task = taskRepository.findById(command.taskId)
            ?: throw TaskNotFoundException("Task with id ${command.taskId} not found")
        task.update(
            title = command.title,
            description = command.description
        )
        taskRepository.save(task)
        return task.toDto()
    }
}
```