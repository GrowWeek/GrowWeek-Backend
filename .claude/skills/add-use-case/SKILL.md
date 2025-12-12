---
name: add-use-case
description: CQRS 패턴에 따라 Command 또는 Query Use Case를 Application Layer에 추가합니다. 상태 변경 또는 조회 작업을 구현할 때 사용하세요.
---

# Add Use Case

## Instructions

### 1. Command Use Case (상태 변경)

**위치**: `application/command/` + `application/service/`

**책임**:
- 상태 변경 작업 (생성, 수정, 삭제)
- 트랜잭션 관리 (`@Transactional`)
- 도메인 객체 조율
- 비즈니스 규칙 검증은 도메인에 위임

**구조**:
```kotlin
// Command DTO
data class CreateTaskCommand(
    val title: String,
    val description: String
)

// Command Handler (Service)
@Service
@Transactional
class TaskCommandService(
    private val taskRepository: TaskRepository
) {
    fun createTask(command: CreateTaskCommand): TaskId {
        val task = Task.create(command.title, command.description)
        return taskRepository.save(task).id
    }
}
```

### 2. Query Use Case (조회)

**위치**: `application/query/` + `application/service/`

**책임**:
- 데이터 조회
- 읽기 전용 (`@Transactional(readOnly = true)`)
- 성능 최적화 (Redis 캐싱, Exposed DSL 최적화)

**구조**:
```kotlin
// Query DTO
data class FindTaskQuery(
    val id: TaskId
)

// Query Handler (Service)
@Service
@Transactional(readOnly = true)
class TaskQueryService(
    private val taskRepository: TaskRepository
) {
    fun findTask(query: FindTaskQuery): TaskSummary? {
        return taskRepository.findById(query.id)
    }
}
```

### 3. 네이밍 규칙

**Command**:
- DTO: `{동사}{명사}Command` (예: `CreateTaskCommand`, `UpdateTaskCommand`)
- Service: `{명사}CommandService` (예: `TaskCommandService`)

**Query**:
- DTO: `{동사}{명사}Query` (예: `FindTaskQuery`, `SearchTasksQuery`)
- Service: `{명사}QueryService` (예: `TaskQueryService`)

### 4. 트랜잭션 관리

- Command: `@Transactional` (기본값: 읽기/쓰기)
- Query: `@Transactional(readOnly = true)` (읽기 전용)

### 5. 검증

**요청 검증** (Controller/Application Layer):
- `@Valid` 애노테이션
- Bean Validation (`@NotNull`, `@Size` 등)

**비즈니스 규칙 검증** (Domain Layer):
- 도메인 모델 내부에서 수행
- `require()`, `check()` 사용
- 위반 시 도메인 예외 발생

## Examples

### Command Use Case 전체 예시

```kotlin
// application/command/CreateTaskCommand.kt
package xyz.robinjoon.growweek.task.application.command

data class CreateTaskCommand(
    val title: String,
    val description: String,
    val assigneeId: String?
)

// application/service/TaskCommandService.kt
package xyz.robinjoon.growweek.task.application.service

@Service
@Transactional
class TaskCommandService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    fun createTask(command: CreateTaskCommand): TaskId {
        // 1. 도메인 객체 생성 (비즈니스 규칙 검증은 도메인에서)
        val task = Task.create(
            title = command.title,
            description = command.description
        )

        // 2. 관련 엔티티 조회 및 연결
        command.assigneeId?.let { assigneeId ->
            val assignee = userRepository.findById(UserId(assigneeId))
                ?: throw UserNotFoundException(assigneeId)
            task.assignTo(assignee)
        }

        // 3. 저장
        return taskRepository.save(task).id
    }

    fun updateTask(taskId: TaskId, command: UpdateTaskCommand): Task {
        val task = taskRepository.findById(taskId)
            ?: throw TaskNotFoundException(taskId)

        task.update(
            title = command.title,
            description = command.description
        )

        return taskRepository.save(task)
    }
}
```

### Query Use Case 전체 예시

```kotlin
// application/query/SearchTasksQuery.kt
package xyz.robinjoon.growweek.task.application.query

data class SearchTasksQuery(
    val status: TaskStatus?,
    val assigneeId: String?,
    val page: Int = 0,
    val size: Int = 20
)

// application/service/TaskQueryService.kt
package xyz.robinjoon.growweek.task.application.service

@Service
@Transactional(readOnly = true)
class TaskQueryService(
    private val taskRepository: TaskRepository
) {
    @Cacheable("tasks", key = "#query.hashCode()")
    fun searchTasks(query: SearchTasksQuery): Page<TaskSummary> {
        return taskRepository.search(
            status = query.status,
            assigneeId = query.assigneeId?.let { UserId(it) },
            pageable = PageRequest.of(query.page, query.size)
        )
    }

    fun findTaskById(taskId: TaskId): TaskDetail? {
        return taskRepository.findDetailById(taskId)
    }
}
```
