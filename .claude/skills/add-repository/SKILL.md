---
name: add-repository
description: Repository 인터페이스와 Exposed ORM 구현체를 추가합니다. Command/Query 추상 클래스를 활용하여 saveAll(command)와 findAll(query) 패턴으로 데이터를 저장/조회합니다. Exposed DSL을 활용합니다.
---

# Add Repository

## Instructions

### 1. Repository 패턴 개요

이 프로젝트의 Repository는 Command/Query Object 패턴을 사용합니다:

- **saveAll(command): List<Domain>** - Command 객체를 받아 도메인 객체들을 저장
- **findAll(query): Page<Domain>** - Query 객체를 받아 페이징된 도메인 객체 반환

각 Command와 Query는 추상 클래스로 정의되며, 구체 타입에 따라 Repository 구현체에서 다른 동작을 수행합니다.

### 2. Command/Query 추상 클래스 정의

**위치**: `{bounded-context}/domain/repository/`

```kotlin
// Command 추상 클래스
abstract class TaskCommand

// 구체 Command 타입들
data class CreateTaskCommand(
    val title: String,
    val description: String,
    val assigneeId: UserId?
) : TaskCommand()

data class UpdateTaskCommand(
    val id: TaskId,
    val title: String,
    val description: String
) : TaskCommand()

data class DeleteTaskCommand(
    val id: TaskId
) : TaskCommand()

// Query 추상 클래스
abstract class TaskQuery

// 구체 Query 타입들
data class FindByIdQuery(
    val id: TaskId
) : TaskQuery()

data class FindByStatusQuery(
    val status: TaskStatus,
    val page: Int = 0,
    val size: Int = 20
) : TaskQuery()

data class SearchByKeywordQuery(
    val keyword: String,
    val page: Int = 0,
    val size: Int = 20
) : TaskQuery()
```

### 3. Repository 인터페이스 정의

**위치**: `{bounded-context}/domain/repository/`

```kotlin
interface TaskRepository {
    fun saveAll(command: TaskCommand): List<Task>
    fun findAll(query: TaskQuery): Page<Task>
}
```

### 4. Exposed ORM Table 정의

**위치**: `{bounded-context}/infrastructure/persistence/`

```kotlin
object TaskTable : LongIdTable("tasks") {
    val title = varchar("title", 100)
    val description = text("description")
    val status = varchar("status", 20)
    val assigneeId = long("assignee_id").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
```

### 5. Repository 구현체

**위치**: `{bounded-context}/infrastructure/persistence/`

**패턴**: `when` 표현식으로 구체 타입에 따라 분기

```kotlin
@Repository
class TaskRepositoryImpl : TaskRepository {

    @Transactional
    override fun saveAll(command: TaskCommand): List<Task> {
        return when (command) {
            is CreateTaskCommand -> handleCreate(command)
            is UpdateTaskCommand -> handleUpdate(command)
            is DeleteTaskCommand -> handleDelete(command)
            else -> throw UnsupportedOperationException(
                "Unsupported command: ${command::class.simpleName}"
            )
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(query: TaskQuery): Page<Task> {
        return when (query) {
            is FindByIdQuery -> handleFindById(query)
            is FindByStatusQuery -> handleFindByStatus(query)
            is SearchByKeywordQuery -> handleSearchByKeyword(query)
            else -> throw UnsupportedOperationException(
                "Unsupported query: ${query::class.simpleName}"
            )
        }
    }
}
```

### 6. 트랜잭션

Repository 메서드에 `@Transactional` 애노테이션 사용:

- **쓰기 작업**: `@Transactional`
- **읽기 작업**: `@Transactional(readOnly = true)`

## Examples

### 완전한 Repository 구현 예시

```kotlin
// domain/repository/TaskCommand.kt
package xyz.robinjoon.growweek.task.domain.repository

sealed class TaskCommand

data class CreateTaskCommand(
    val title: String,
    val description: String,
    val assigneeId: UserId? = null
) : TaskCommand()

data class UpdateTaskCommand(
    val id: TaskId,
    val title: String,
    val description: String
) : TaskCommand()

data class CompleteTaskCommand(
    val id: TaskId
) : TaskCommand()

data class DeleteTaskCommand(
    val id: TaskId
) : TaskCommand()

data class BulkCreateTaskCommand(
    val tasks: List<CreateTaskCommand>
) : TaskCommand()

// domain/repository/TaskQuery.kt
package xyz.robinjoon.growweek.task.domain.repository

sealed class TaskQuery

data class FindByIdQuery(
    val id: TaskId
) : TaskQuery()

data class FindByStatusQuery(
    val status: TaskStatus,
    val page: Int = 0,
    val size: Int = 20
) : TaskQuery()

data class FindByAssigneeQuery(
    val assigneeId: UserId,
    val page: Int = 0,
    val size: Int = 20
) : TaskQuery()

data class SearchByKeywordQuery(
    val keyword: String,
    val statuses: List<TaskStatus> = emptyList(),
    val page: Int = 0,
    val size: Int = 20
) : TaskQuery()

data class FindAllQuery(
    val page: Int = 0,
    val size: Int = 20
) : TaskQuery()

// domain/repository/TaskRepository.kt
package xyz.robinjoon.growweek.task.domain.repository

interface TaskRepository {
    fun saveAll(command: TaskCommand): List<Task>
    fun findAll(query: TaskQuery): Page<Task>
}

// infrastructure/persistence/TaskTable.kt
package xyz.robinjoon.growweek.task.infrastructure.persistence

object TaskTable : LongIdTable("tasks") {
    val title = varchar("title", 100)
    val description = text("description")
    val status = varchar("status", 20)
    val assigneeId = long("assignee_id").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

// infrastructure/persistence/TaskRepositoryImpl.kt
package xyz.robinjoon.growweek.task.infrastructure.persistence

@Repository
class TaskRepositoryImpl : TaskRepository {

    @Transactional
    override fun saveAll(command: TaskCommand): List<Task> {
        return when (command) {
            is CreateTaskCommand -> listOf(handleCreate(command))
            is UpdateTaskCommand -> listOf(handleUpdate(command))
            is CompleteTaskCommand -> listOf(handleComplete(command))
            is DeleteTaskCommand -> {
                handleDelete(command)
                emptyList()
            }
            is BulkCreateTaskCommand -> handleBulkCreate(command)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(query: TaskQuery): Page<Task> {
        return when (query) {
            is FindByIdQuery -> handleFindById(query)
            is FindByStatusQuery -> handleFindByStatus(query)
            is FindByAssigneeQuery -> handleFindByAssignee(query)
            is SearchByKeywordQuery -> handleSearchByKeyword(query)
            is FindAllQuery -> handleFindAll(query)
        }
    }

    // === Command Handlers ===

    private fun handleCreate(command: CreateTaskCommand): Task {
        val now = LocalDateTime.now()
        val id = TaskTable.insertAndGetId {
            it[title] = command.title
            it[description] = command.description
            it[status] = TaskStatus.PENDING.name
            it[assigneeId] = command.assigneeId?.value
            it[createdAt] = now
            it[updatedAt] = now
        }

        return Task(
            id = TaskId(id.value),
            title = command.title,
            description = command.description,
            status = TaskStatus.PENDING,
            assigneeId = command.assigneeId,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun handleUpdate(command: UpdateTaskCommand): Task {
        val existing = TaskTable
            .select { TaskTable.id eq command.id.value }
            .singleOrNull()
            ?: throw TaskNotFoundException(command.id)

        TaskTable.update({ TaskTable.id eq command.id.value }) {
            it[title] = command.title
            it[description] = command.description
            it[updatedAt] = LocalDateTime.now()
        }

        return Task(
            id = command.id,
            title = command.title,
            description = command.description,
            status = TaskStatus.valueOf(existing[TaskTable.status]),
            assigneeId = existing[TaskTable.assigneeId]?.let { TaskId(it) },
            createdAt = existing[TaskTable.createdAt],
            updatedAt = LocalDateTime.now()
        )
    }

    private fun handleComplete(command: CompleteTaskCommand): Task {
        val existing = TaskTable
            .select { TaskTable.id eq command.id.value }
            .singleOrNull()
            ?: throw TaskNotFoundException(command.id)

        TaskTable.update({ TaskTable.id eq command.id.value }) {
            it[status] = TaskStatus.COMPLETED.name
            it[updatedAt] = LocalDateTime.now()
        }

        return toTask(
            TaskTable.select { TaskTable.id eq command.id.value }.single()
        )
    }

    private fun handleDelete(command: DeleteTaskCommand) {
        TaskTable.deleteWhere { TaskTable.id eq command.id.value }
    }

    private fun handleBulkCreate(command: BulkCreateTaskCommand): List<Task> {
        return command.tasks.map { handleCreate(it) }
    }

    // === Query Handlers ===

    private fun handleFindById(query: FindByIdQuery): Page<Task> {
        val task = TaskTable
            .select { TaskTable.id eq query.id.value }
            .singleOrNull()
            ?.let { toTask(it) }

        return if (task != null) {
            PageImpl(listOf(task), PageRequest.of(0, 1), 1)
        } else {
            PageImpl(emptyList(), PageRequest.of(0, 1), 0)
        }
    }

    private fun handleFindByStatus(query: FindByStatusQuery): Page<Task> {
        val dbQuery = TaskTable
            .select { TaskTable.status eq query.status.name }
            .orderBy(TaskTable.createdAt to SortOrder.DESC)

        val total = dbQuery.count()
        val items = dbQuery
            .limit(query.size, (query.page * query.size).toLong())
            .map { toTask(it) }

        return PageImpl(items, PageRequest.of(query.page, query.size), total)
    }

    private fun handleFindByAssignee(query: FindByAssigneeQuery): Page<Task> {
        val dbQuery = TaskTable
            .select { TaskTable.assigneeId eq query.assigneeId.value }
            .orderBy(TaskTable.createdAt to SortOrder.DESC)

        val total = dbQuery.count()
        val items = dbQuery
            .limit(query.size, (query.page * query.size).toLong())
            .map { toTask(it) }

        return PageImpl(items, PageRequest.of(query.page, query.size), total)
    }

    private fun handleSearchByKeyword(query: SearchByKeywordQuery): Page<Task> {
        val dbQuery = TaskTable
            .selectAll()
            .where {
                (TaskTable.title like "%${query.keyword}%") or
                (TaskTable.description like "%${query.keyword}%")
            }
            .apply {
                if (query.statuses.isNotEmpty()) {
                    andWhere {
                        TaskTable.status inList query.statuses.map { it.name }
                    }
                }
            }
            .orderBy(TaskTable.createdAt to SortOrder.DESC)

        val total = dbQuery.count()
        val items = dbQuery
            .limit(query.size, (query.page * query.size).toLong())
            .map { toTask(it) }

        return PageImpl(items, PageRequest.of(query.page, query.size), total)
    }

    private fun handleFindAll(query: FindAllQuery): Page<Task> {
        val dbQuery = TaskTable
            .selectAll()
            .orderBy(TaskTable.createdAt to SortOrder.DESC)

        val total = dbQuery.count()
        val items = dbQuery
            .limit(query.size, (query.page * query.size).toLong())
            .map { toTask(it) }

        return PageImpl(items, PageRequest.of(query.page, query.size), total)
    }

    // === Mapper ===

    private fun toTask(row: ResultRow): Task {
        return Task(
            id = TaskId(row[TaskTable.id].value),
            title = row[TaskTable.title],
            description = row[TaskTable.description],
            status = TaskStatus.valueOf(row[TaskTable.status]),
            assigneeId = row[TaskTable.assigneeId]?.let { UserId(it) },
            createdAt = row[TaskTable.createdAt],
            updatedAt = row[TaskTable.updatedAt]
        )
    }
}
```

### Application Service에서 사용 예시

```kotlin
// application/service/TaskCommandService.kt
@Service
@Transactional
class TaskCommandService(
    private val taskRepository: TaskRepository
) {
    fun createTask(request: CreateTaskRequest): TaskId {
        val command = CreateTaskCommand(
            title = request.title,
            description = request.description,
            assigneeId = request.assigneeId?.let { UserId(it) }
        )

        val tasks = taskRepository.saveAll(command)
        return tasks.first().id
    }

    fun updateTask(taskId: TaskId, request: UpdateTaskRequest): Task {
        val command = UpdateTaskCommand(
            id = taskId,
            title = request.title,
            description = request.description
        )

        val tasks = taskRepository.saveAll(command)
        return tasks.first()
    }

    fun completeTask(taskId: TaskId): Task {
        val command = CompleteTaskCommand(id = taskId)
        val tasks = taskRepository.saveAll(command)
        return tasks.first()
    }

    fun deleteTask(taskId: TaskId) {
        val command = DeleteTaskCommand(id = taskId)
        taskRepository.saveAll(command)
    }

    fun bulkCreateTasks(requests: List<CreateTaskRequest>): List<TaskId> {
        val command = BulkCreateTaskCommand(
            tasks = requests.map {
                CreateTaskCommand(
                    title = it.title,
                    description = it.description,
                    assigneeId = it.assigneeId?.let { id -> UserId(id) }
                )
            }
        )

        return taskRepository.saveAll(command).map { it.id }
    }
}

// application/service/TaskQueryService.kt
@Service
@Transactional(readOnly = true)
class TaskQueryService(
    private val taskRepository: TaskRepository
) {
    fun findTaskById(taskId: TaskId): Task? {
        val query = FindByIdQuery(id = taskId)
        val page = taskRepository.findAll(query)
        return page.content.firstOrNull()
    }

    fun findTasksByStatus(status: TaskStatus, page: Int, size: Int): Page<Task> {
        val query = FindByStatusQuery(
            status = status,
            page = page,
            size = size
        )
        return taskRepository.findAll(query)
    }

    fun searchTasks(
        keyword: String,
        statuses: List<TaskStatus>,
        page: Int,
        size: Int
    ): Page<Task> {
        val query = SearchByKeywordQuery(
            keyword = keyword,
            statuses = statuses,
            page = page,
            size = size
        )
        return taskRepository.findAll(query)
    }
}
```

### 장점

1. **유연성**: 새로운 Command/Query 타입을 추가하기 쉬움
2. **타입 안전성**: sealed class로 모든 케이스를 컴파일 타임에 검증
3. **명확한 의도**: Command와 Query가 명시적으로 분리됨
4. **확장성**: 복잡한 조회 조건도 Query 객체로 표현 가능
5. **일관성**: 모든 Repository가 동일한 인터페이스 제공
