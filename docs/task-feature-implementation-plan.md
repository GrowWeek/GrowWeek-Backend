# 할일(Task) 기능 구현 작업 계획

## 1. 개요

GrowWeek 프로젝트에 주간 할일 관리 및 회고 기능을 구현합니다. 본 문서는 DDD, Clean Architecture, CQRS 패턴을 적용한 구현 계획을 제시합니다.

### 참고 문서
- [전체 플로우](https://www.notion.so/robinjoon/2cb26f51b6c080f298c8cb701bdbe7de)
- [할일 비즈니스 규칙](https://www.notion.so/robinjoon/2cb26f51b6c08048a835d1f202c20dfe)

## 2. 비즈니스 요구사항

### 2.1 전체 플로우

1. **매주 할일 작성**: 사용자가 주간 할일을 생성
2. **매일 할일 관리**: 할일 추가/수정, 칸반 차트 상태 이동, 코멘트 작성
3. **매주 금요일 회고 작성**:
   - AI가 할일 목록 및 상태를 분석하여 회고 질문 생성
   - 민감한 할일은 단계에 따라 AI에 전달되는 정보 제한
   - 사용자가 생성된 질문에 답변하며 회고 작성
4. **매월 회고 조회**: 월별로 회고를 모아서 조회

### 2.2 할일 데이터 구조

| 필드 | 타입 | 제약사항 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 할일 ID |
| title | String | 50자 이하, Not Null | 할일 제목 |
| description | String | 3000자 이하, Nullable | 상세 설명 |
| status | Enum | Not Null | 진행 상태 (TODO, IN_PROGRESS, DONE, CANCEL) |
| sensitivityLevel | Enum | Not Null, Default: NONE | 민감도 단계 (NONE, TITLE_ONLY, NEVER) |
| priority | Int | Not Null | 중요도 |
| startDate | LocalDate | Not Null | 시작일 |
| dueDate | LocalDate | Not Null | 마감일 |
| userId | Long | FK, Not Null | 사용자 ID |
| createdAt | Timestamp | Not Null | 생성 시각 |
| updatedAt | Timestamp | Not Null | 수정 시각 |

### 2.3 할일 진행 상태 (TaskStatus)

```kotlin
enum class TaskStatus {
    TODO,        // 할 일
    IN_PROGRESS, // 진행 중
    DONE,        // 완료
    CANCEL       // 취소
}
```

- 모든 상태 간 이동 제약 없음 (자유로운 상태 전환)

### 2.4 민감도 단계 (SensitivityLevel)

```kotlin
enum class SensitivityLevel {
    NONE,        // 민감하지 않음 - 모든 정보 AI에 전달
    TITLE_ONLY,  // 제목만 전달 - 제목만 AI에 전달
    NEVER        // 전달하지 않음 - AI에 전달하지 않음
}
```

### 2.5 할일 수정 제한 조건

1. **회고가 작성된 할일은 기본적으로 수정 불가**
2. **단, 할일의 마감일이 회고 시점 이후인 경우 제한적으로 수정 가능**:
   - 시작일 수정 불가
   - 회고 시점 이전으로 마감일 수정 불가
   - 그 외 정보(제목, 설명, 상태, 민감도, 중요도)는 수정 가능

### 2.6 주간 할일 판단 기준

- 시작일과 마감일 범위가 특정 주에 걸친다면 해당 주의 할일로 간주
- 단, 설정된 마감일 이전에 완료(DONE)된 경우 해당 주에 포함되지 않음

## 3. 도메인 모델 설계

### 3.1 Bounded Context

- **Context Name**: `task`
- **Package**: `xyz.robinjoon.growweek.task`

### 3.2 Aggregate Root

#### Task (할일)

```kotlin
// domain/model/command/Task.kt
data class Task(
    val id: TaskId,
    val userId: UserId,
    val title: TaskTitle,
    val description: TaskDescription?,
    val status: TaskStatus,
    val sensitivityLevel: SensitivityLevel,
    val priority: Priority,
    val period: TaskPeriod,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val retrospectiveId: RetrospectiveId? = null
) {
    fun canModify(): Boolean {
        // 회고가 없으면 수정 가능
        if (retrospectiveId == null) return true
        // 마감일이 현재 시점 이후면 제한적 수정 가능
        return period.dueDate.isAfter(LocalDate.now())
    }

    fun updateTitle(newTitle: TaskTitle, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(title = newTitle, updatedAt = Timestamp.now())
    }

    fun updateStatus(newStatus: TaskStatus, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(status = newStatus, updatedAt = Timestamp.now())
    }

    fun updateDueDate(newDueDate: LocalDate, retrospectiveDate: LocalDate?): Task {
        if (retrospectiveId != null && retrospectiveDate != null) {
            require(newDueDate.isAfter(retrospectiveDate)) {
                "회고 시점 이전으로 마감일을 수정할 수 없습니다"
            }
        }
        val newPeriod = period.copy(dueDate = newDueDate)
        return copy(period = newPeriod, updatedAt = Timestamp.now())
    }

    private fun validateModification(retrospectiveDate: LocalDate?) {
        if (retrospectiveId != null && retrospectiveDate != null) {
            require(period.dueDate.isAfter(retrospectiveDate)) {
                "회고가 작성된 할일은 수정할 수 없습니다"
            }
        }
    }

    fun belongsToWeek(weekStart: LocalDate, weekEnd: LocalDate): Boolean {
        // 시작일~마감일 범위가 해당 주와 겹치는지 확인
        return period.overlaps(weekStart, weekEnd)
    }
}
```

### 3.3 Value Objects

```kotlin
// domain/model/command/TaskId.kt
@JvmInline
value class TaskId(val value: Long)

// domain/model/command/TaskTitle.kt
@JvmInline
value class TaskTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "제목은 비어있을 수 없습니다" }
        require(value.length <= 50) { "제목은 50자 이하여야 합니다" }
    }
}

// domain/model/command/TaskDescription.kt
@JvmInline
value class TaskDescription(val value: String) {
    init {
        require(value.length <= 3000) { "설명은 3000자 이하여야 합니다" }
    }
}

// domain/model/command/Priority.kt
@JvmInline
value class Priority(val value: Int) {
    init {
        require(value >= 1) { "중요도는 1 이상이어야 합니다" }
    }
}

// domain/model/command/TaskPeriod.kt
data class TaskPeriod(
    val startDate: LocalDate,
    val dueDate: LocalDate
) {
    init {
        require(!dueDate.isBefore(startDate)) {
            "마감일은 시작일보다 이전일 수 없습니다"
        }
    }

    fun overlaps(weekStart: LocalDate, weekEnd: LocalDate): Boolean {
        return !(dueDate.isBefore(weekStart) || startDate.isAfter(weekEnd))
    }
}
```

### 3.4 Query Models

```kotlin
// domain/model/query/TaskSummary.kt
data class TaskSummary(
    val id: Long,
    val userId: Long,
    val title: String,
    val status: TaskStatus,
    val priority: Int,
    val startDate: LocalDate,
    val dueDate: LocalDate
)

// domain/model/query/TaskDetail.kt
data class TaskDetail(
    val id: Long,
    val userId: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val sensitivityLevel: SensitivityLevel,
    val priority: Int,
    val startDate: LocalDate,
    val dueDate: LocalDate,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val hasRetrospective: Boolean
)

// domain/model/query/WeeklyTaskSummary.kt
data class WeeklyTaskSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val tasks: List<TaskSummary>,
    val statusCount: Map<TaskStatus, Int>
)
```

## 4. 계층별 구현 계획

### 4.1 Domain Layer

#### 4.1.1 Repository Interfaces

```kotlin
// domain/repository/TaskCommandRepository.kt
interface TaskCommandRepository {
    fun save(task: Task): Task
    fun findById(id: TaskId): Task?
    fun delete(id: TaskId)
    fun findByIdAndUserId(id: TaskId, userId: UserId): Task?
}

// domain/repository/TaskQueryRepository.kt
interface TaskQueryRepository {
    fun findDetailById(id: Long): TaskDetail?
    fun findWeeklyTasks(userId: Long, weekStart: LocalDate, weekEnd: LocalDate): List<TaskSummary>
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<TaskSummary>
}
```

#### 4.1.2 Domain Services

```kotlin
// domain/service/TaskDomainService.kt
class TaskDomainService {
    fun calculateWeekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val weekStart = date.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        return weekStart to weekEnd
    }

    fun isTaskInWeek(task: Task, weekStart: LocalDate, weekEnd: LocalDate): Boolean {
        // 해당 주에 포함되는지 확인
        if (!task.period.overlaps(weekStart, weekEnd)) return false
        // 마감일 이전에 완료된 경우 제외
        if (task.status == TaskStatus.DONE && task.updatedAt.toLocalDate().isBefore(task.period.dueDate)) {
            return false
        }
        return true
    }
}
```

### 4.2 Application Layer

#### 4.2.1 Commands

```kotlin
// application/command/CreateTaskCommand.kt
data class CreateTaskCommand(
    val userId: Long,
    val title: String,
    val description: String?,
    val priority: Int,
    val startDate: LocalDate,
    val dueDate: LocalDate,
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.NONE
)

// application/command/UpdateTaskCommand.kt
data class UpdateTaskCommand(
    val taskId: Long,
    val userId: Long,
    val title: String?,
    val description: String?,
    val status: TaskStatus?,
    val priority: Int?,
    val dueDate: LocalDate?,
    val sensitivityLevel: SensitivityLevel?
)

// application/command/UpdateTaskStatusCommand.kt
data class UpdateTaskStatusCommand(
    val taskId: Long,
    val userId: Long,
    val status: TaskStatus
)
```

#### 4.2.2 Queries

```kotlin
// application/query/GetTaskQuery.kt
data class GetTaskQuery(
    val taskId: Long,
    val userId: Long
)

// application/query/GetWeeklyTasksQuery.kt
data class GetWeeklyTasksQuery(
    val userId: Long,
    val weekStart: LocalDate,
    val weekEnd: LocalDate
) : PageQuery {
    override val page: Int = 0
    override val size: Int = Int.MAX_VALUE
}

// application/query/GetUserTasksQuery.kt
data class GetUserTasksQuery(
    val userId: Long,
    override val page: Int = 0,
    override val size: Int = 20
) : PageQuery
```

#### 4.2.3 DTOs

```kotlin
// application/dto/TaskResponse.kt
data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val sensitivityLevel: SensitivityLevel,
    val priority: Int,
    val startDate: LocalDate,
    val dueDate: LocalDate,
    val hasRetrospective: Boolean,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)

// application/dto/WeeklyTaskResponse.kt
data class WeeklyTaskResponse(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val tasks: List<TaskResponse>,
    val statistics: TaskStatistics
)

data class TaskStatistics(
    val total: Int,
    val todo: Int,
    val inProgress: Int,
    val done: Int,
    val cancel: Int
)
```

#### 4.2.4 Use Cases

```kotlin
// application/usecase/CreateTaskUseCase.kt
interface CreateTaskUseCase {
    fun execute(command: CreateTaskCommand): TaskResponse
}

// application/usecase/UpdateTaskUseCase.kt
interface UpdateTaskUseCase {
    fun execute(command: UpdateTaskCommand): TaskResponse
}

// application/usecase/UpdateTaskStatusUseCase.kt
interface UpdateTaskStatusUseCase {
    fun execute(command: UpdateTaskStatusCommand): TaskResponse
}

// application/usecase/DeleteTaskUseCase.kt
interface DeleteTaskUseCase {
    fun execute(taskId: Long, userId: Long)
}

// application/usecase/GetTaskUseCase.kt
interface GetTaskUseCase {
    fun execute(query: GetTaskQuery): TaskResponse?
}

// application/usecase/GetWeeklyTasksUseCase.kt
interface GetWeeklyTasksUseCase {
    fun execute(query: GetWeeklyTasksQuery): WeeklyTaskResponse
}

// application/usecase/GetUserTasksUseCase.kt
interface GetUserTasksUseCase {
    fun execute(query: GetUserTasksQuery): Page<TaskResponse>
}
```

#### 4.2.5 Service Implementations

```kotlin
// application/service/CreateTaskService.kt
@Service
@Transactional
class CreateTaskService(
    private val taskCommandRepository: TaskCommandRepository
) : CreateTaskUseCase {
    override fun execute(command: CreateTaskCommand): TaskResponse {
        val task = Task(
            id = TaskId(0), // Auto-generated
            userId = UserId(command.userId),
            title = TaskTitle(command.title),
            description = command.description?.let { TaskDescription(it) },
            status = TaskStatus.TODO,
            sensitivityLevel = command.sensitivityLevel,
            priority = Priority(command.priority),
            period = TaskPeriod(command.startDate, command.dueDate),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        val saved = taskCommandRepository.save(task)
        return saved.toResponse()
    }
}

// application/service/UpdateTaskService.kt
@Service
@Transactional
class UpdateTaskService(
    private val taskCommandRepository: TaskCommandRepository
) : UpdateTaskUseCase {
    override fun execute(command: UpdateTaskCommand): TaskResponse {
        val task = taskCommandRepository.findByIdAndUserId(
            TaskId(command.taskId),
            UserId(command.userId)
        ) ?: throw TaskNotFoundException(command.taskId)

        var updated = task

        command.title?.let {
            updated = updated.updateTitle(TaskTitle(it), null)
        }
        command.status?.let {
            updated = updated.updateStatus(it, null)
        }
        command.dueDate?.let {
            updated = updated.updateDueDate(it, null)
        }
        // ... 기타 필드 업데이트

        val saved = taskCommandRepository.save(updated)
        return saved.toResponse()
    }
}

// application/service/GetWeeklyTasksService.kt
@Service
@Transactional(readOnly = true)
class GetWeeklyTasksService(
    private val taskQueryRepository: TaskQueryRepository,
    private val taskDomainService: TaskDomainService
) : GetWeeklyTasksUseCase {
    override fun execute(query: GetWeeklyTasksQuery): WeeklyTaskResponse {
        val tasks = taskQueryRepository.findWeeklyTasks(
            query.userId,
            query.weekStart,
            query.weekEnd
        )

        val statistics = calculateStatistics(tasks)

        return WeeklyTaskResponse(
            weekStart = query.weekStart,
            weekEnd = query.weekEnd,
            tasks = tasks.map { it.toResponse() },
            statistics = statistics
        )
    }

    private fun calculateStatistics(tasks: List<TaskSummary>): TaskStatistics {
        val statusCount = tasks.groupingBy { it.status }.eachCount()
        return TaskStatistics(
            total = tasks.size,
            todo = statusCount[TaskStatus.TODO] ?: 0,
            inProgress = statusCount[TaskStatus.IN_PROGRESS] ?: 0,
            done = statusCount[TaskStatus.DONE] ?: 0,
            cancel = statusCount[TaskStatus.CANCEL] ?: 0
        )
    }
}
```

### 4.3 Infrastructure Layer

#### 4.3.1 Exposed Table Definition

```kotlin
// infrastructure/persistence/TaskTable.kt
object TaskTable : LongIdTable("tasks") {
    val userId = long("user_id").index()
    val title = varchar("title", 50)
    val description = text("description").nullable()
    val status = enumerationByName("status", 20, TaskStatus::class)
    val sensitivityLevel = enumerationByName("sensitivity_level", 20, SensitivityLevel::class)
    val priority = integer("priority")
    val startDate = date("start_date")
    val dueDate = date("due_date")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val retrospectiveId = long("retrospective_id").nullable().index()

    init {
        index(false, userId, startDate, dueDate)
    }
}
```

#### 4.3.2 Repository Implementations

```kotlin
// infrastructure/persistence/ExposedTaskCommandRepository.kt
@Repository
class ExposedTaskCommandRepository : TaskCommandRepository {
    override fun save(task: Task): Task {
        val id = if (task.id.value == 0L) {
            TaskTable.insertAndGetId {
                it[userId] = task.userId.value
                it[title] = task.title.value
                it[description] = task.description?.value
                it[status] = task.status
                it[sensitivityLevel] = task.sensitivityLevel
                it[priority] = task.priority.value
                it[startDate] = task.period.startDate
                it[dueDate] = task.period.dueDate
                it[createdAt] = task.createdAt
                it[updatedAt] = task.updatedAt
                it[retrospectiveId] = task.retrospectiveId?.value
            }.value
        } else {
            TaskTable.update({ TaskTable.id eq task.id.value }) {
                it[userId] = task.userId.value
                it[title] = task.title.value
                it[description] = task.description?.value
                it[status] = task.status
                it[sensitivityLevel] = task.sensitivityLevel
                it[priority] = task.priority.value
                it[startDate] = task.period.startDate
                it[dueDate] = task.period.dueDate
                it[updatedAt] = task.updatedAt
                it[retrospectiveId] = task.retrospectiveId?.value
            }
            task.id.value
        }

        return task.copy(id = TaskId(id))
    }

    override fun findById(id: TaskId): Task? {
        return TaskTable.select { TaskTable.id eq id.value }
            .map { it.toTask() }
            .singleOrNull()
    }

    override fun findByIdAndUserId(id: TaskId, userId: UserId): Task? {
        return TaskTable.select {
            (TaskTable.id eq id.value) and (TaskTable.userId eq userId.value)
        }
            .map { it.toTask() }
            .singleOrNull()
    }

    override fun delete(id: TaskId) {
        TaskTable.deleteWhere { TaskTable.id eq id.value }
    }

    private fun ResultRow.toTask(): Task {
        return Task(
            id = TaskId(this[TaskTable.id].value),
            userId = UserId(this[TaskTable.userId]),
            title = TaskTitle(this[TaskTable.title]),
            description = this[TaskTable.description]?.let { TaskDescription(it) },
            status = this[TaskTable.status],
            sensitivityLevel = this[TaskTable.sensitivityLevel],
            priority = Priority(this[TaskTable.priority]),
            period = TaskPeriod(this[TaskTable.startDate], this[TaskTable.dueDate]),
            createdAt = this[TaskTable.createdAt],
            updatedAt = this[TaskTable.updatedAt],
            retrospectiveId = this[TaskTable.retrospectiveId]?.let { RetrospectiveId(it) }
        )
    }
}

// infrastructure/persistence/ExposedTaskQueryRepository.kt
@Repository
class ExposedTaskQueryRepository : TaskQueryRepository {
    override fun findDetailById(id: Long): TaskDetail? {
        return TaskTable.select { TaskTable.id eq id }
            .map { it.toTaskDetail() }
            .singleOrNull()
    }

    override fun findWeeklyTasks(
        userId: Long,
        weekStart: LocalDate,
        weekEnd: LocalDate
    ): List<TaskSummary> {
        return TaskTable.select {
            (TaskTable.userId eq userId) and
            (TaskTable.startDate lessEq weekEnd) and
            (TaskTable.dueDate greaterEq weekStart)
        }
            .orderBy(TaskTable.priority to SortOrder.DESC, TaskTable.dueDate to SortOrder.ASC)
            .map { it.toTaskSummary() }
    }

    override fun findAllByUserId(userId: Long, pageable: Pageable): Page<TaskSummary> {
        val total = TaskTable.select { TaskTable.userId eq userId }.count()

        val tasks = TaskTable.select { TaskTable.userId eq userId }
            .orderBy(TaskTable.updatedAt to SortOrder.DESC)
            .limit(pageable.pageSize, pageable.offset)
            .map { it.toTaskSummary() }

        return PageImpl(tasks, pageable, total)
    }

    private fun ResultRow.toTaskSummary(): TaskSummary {
        return TaskSummary(
            id = this[TaskTable.id].value,
            userId = this[TaskTable.userId],
            title = this[TaskTable.title],
            status = this[TaskTable.status],
            priority = this[TaskTable.priority],
            startDate = this[TaskTable.startDate],
            dueDate = this[TaskTable.dueDate]
        )
    }

    private fun ResultRow.toTaskDetail(): TaskDetail {
        return TaskDetail(
            id = this[TaskTable.id].value,
            userId = this[TaskTable.userId],
            title = this[TaskTable.title],
            description = this[TaskTable.description],
            status = this[TaskTable.status],
            sensitivityLevel = this[TaskTable.sensitivityLevel],
            priority = this[TaskTable.priority],
            startDate = this[TaskTable.startDate],
            dueDate = this[TaskTable.dueDate],
            createdAt = this[TaskTable.createdAt],
            updatedAt = this[TaskTable.updatedAt],
            hasRetrospective = this[TaskTable.retrospectiveId] != null
        )
    }
}
```

### 4.4 Presentation Layer

#### 4.4.1 Request DTOs

```kotlin
// presentation/rest/request/CreateTaskRequest.kt
data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val priority: Int,
    val startDate: String, // yyyy-MM-dd
    val dueDate: String,   // yyyy-MM-dd
    val sensitivityLevel: String? = "NONE"
)

// presentation/rest/request/UpdateTaskRequest.kt
data class UpdateTaskRequest(
    val title: String?,
    val description: String?,
    val status: String?,
    val priority: Int?,
    val dueDate: String?,
    val sensitivityLevel: String?
)

// presentation/rest/request/UpdateTaskStatusRequest.kt
data class UpdateTaskStatusRequest(
    val status: String
)
```

#### 4.4.2 Response DTOs

```kotlin
// presentation/rest/response/TaskResponse.kt
data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val sensitivityLevel: String,
    val priority: Int,
    val startDate: String,
    val dueDate: String,
    val hasRetrospective: Boolean,
    val createdAt: String,
    val updatedAt: String
)

// presentation/rest/response/WeeklyTaskResponse.kt
data class WeeklyTaskResponse(
    val weekStart: String,
    val weekEnd: String,
    val tasks: List<TaskResponse>,
    val statistics: TaskStatisticsResponse
)

data class TaskStatisticsResponse(
    val total: Int,
    val todo: Int,
    val inProgress: Int,
    val done: Int,
    val cancel: Int
)
```

#### 4.4.3 Controllers

```kotlin
// presentation/rest/controller/TaskController.kt
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getTaskUseCase: GetTaskUseCase,
    private val getWeeklyTasksUseCase: GetWeeklyTasksUseCase,
    private val getUserTasksUseCase: GetUserTasksUseCase
) {
    @PostMapping
    @Operation(summary = "할일 생성", description = "새로운 할일을 생성합니다")
    fun createTask(
        @RequestBody request: CreateTaskRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<TaskResponse> {
        val command = request.toCommand(userId)
        val response = createTaskUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(response.toResponse())
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "할일 수정", description = "할일 정보를 수정합니다")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<TaskResponse> {
        val command = request.toCommand(taskId, userId)
        val response = updateTaskUseCase.execute(command)
        return ResponseEntity.ok(response.toResponse())
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "할일 상태 변경", description = "할일의 진행 상태를 변경합니다")
    fun updateTaskStatus(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskStatusRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<TaskResponse> {
        val command = UpdateTaskStatusCommand(
            taskId = taskId,
            userId = userId,
            status = TaskStatus.valueOf(request.status)
        )
        val response = updateTaskStatusUseCase.execute(command)
        return ResponseEntity.ok(response.toResponse())
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "할일 삭제", description = "할일을 삭제합니다")
    fun deleteTask(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<Void> {
        deleteTaskUseCase.execute(taskId, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "할일 조회", description = "할일 상세 정보를 조회합니다")
    fun getTask(
        @PathVariable taskId: Long,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<TaskResponse> {
        val query = GetTaskQuery(taskId, userId)
        val response = getTaskUseCase.execute(query)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(response.toResponse())
    }

    @GetMapping("/weekly")
    @Operation(summary = "주간 할일 조회", description = "특정 주의 할일 목록을 조회합니다")
    fun getWeeklyTasks(
        @RequestParam weekStart: String,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<WeeklyTaskResponse> {
        val startDate = LocalDate.parse(weekStart)
        val endDate = startDate.plusDays(6)
        val query = GetWeeklyTasksQuery(userId, startDate, endDate)
        val response = getWeeklyTasksUseCase.execute(query)
        return ResponseEntity.ok(response.toResponse())
    }

    @GetMapping
    @Operation(summary = "할일 목록 조회", description = "사용자의 전체 할일 목록을 페이징하여 조회합니다")
    fun getUserTasks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<Page<TaskResponse>> {
        val query = GetUserTasksQuery(userId, page, size)
        val response = getUserTasksUseCase.execute(query)
        return ResponseEntity.ok(response.map { it.toResponse() })
    }
}
```

## 5. 데이터베이스 스키마

### 5.1 DDL

```sql
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'NONE',
    priority INTEGER NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retrospective_id BIGINT,

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_retrospective FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id)
);

CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_retrospective_id ON tasks(retrospective_id);
CREATE INDEX idx_tasks_user_dates ON tasks(user_id, start_date, due_date);
```

## 6. API 명세

### 6.1 할일 생성

```
POST /api/v1/tasks
```

**Request Body**:
```json
{
  "title": "할일 제목",
  "description": "할일 상세 설명",
  "priority": 5,
  "startDate": "2025-12-17",
  "dueDate": "2025-12-23",
  "sensitivityLevel": "NONE"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "title": "할일 제목",
  "description": "할일 상세 설명",
  "status": "TODO",
  "sensitivityLevel": "NONE",
  "priority": 5,
  "startDate": "2025-12-17",
  "dueDate": "2025-12-23",
  "hasRetrospective": false,
  "createdAt": "2025-12-17T10:00:00",
  "updatedAt": "2025-12-17T10:00:00"
}
```

### 6.2 할일 수정

```
PUT /api/v1/tasks/{taskId}
```

**Request Body**:
```json
{
  "title": "수정된 제목",
  "description": "수정된 설명",
  "status": "IN_PROGRESS",
  "priority": 7,
  "dueDate": "2025-12-25",
  "sensitivityLevel": "TITLE_ONLY"
}
```

### 6.3 할일 상태 변경

```
PATCH /api/v1/tasks/{taskId}/status
```

**Request Body**:
```json
{
  "status": "DONE"
}
```

### 6.4 할일 삭제

```
DELETE /api/v1/tasks/{taskId}
```

**Response**: 204 No Content

### 6.5 할일 조회

```
GET /api/v1/tasks/{taskId}
```

### 6.6 주간 할일 조회

```
GET /api/v1/tasks/weekly?weekStart=2025-12-16
```

**Response**:
```json
{
  "weekStart": "2025-12-16",
  "weekEnd": "2025-12-22",
  "tasks": [...],
  "statistics": {
    "total": 10,
    "todo": 3,
    "inProgress": 4,
    "done": 2,
    "cancel": 1
  }
}
```

### 6.7 할일 목록 조회 (페이징)

```
GET /api/v1/tasks?page=0&size=20
```

## 7. 예외 처리

### 7.1 커스텀 예외

```kotlin
// common/exception/TaskException.kt
sealed class TaskException(message: String) : RuntimeException(message)

class TaskNotFoundException(taskId: Long) :
    TaskException("할일을 찾을 수 없습니다: $taskId")

class TaskModificationNotAllowedException(message: String) :
    TaskException(message)

class InvalidTaskPeriodException(message: String) :
    TaskException(message)
```

### 7.2 예외 핸들러

```kotlin
// common/exception/GlobalExceptionHandler.kt
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException::class)
    fun handleTaskNotFound(ex: TaskNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("TASK_NOT_FOUND", ex.message))
    }

    @ExceptionHandler(TaskModificationNotAllowedException::class)
    fun handleModificationNotAllowed(ex: TaskModificationNotAllowedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse("MODIFICATION_NOT_ALLOWED", ex.message))
    }
}
```

## 8. 테스트 계획

### 8.1 단위 테스트

- **Domain Layer**:
  - Value Object 검증 로직 테스트
  - Task Entity 비즈니스 로직 테스트
  - TaskDomainService 테스트

- **Application Layer**:
  - 각 Use Case 서비스 테스트
  - Command/Query 처리 로직 테스트

### 8.2 통합 테스트

- **Repository Tests**:
  - Exposed ORM 쿼리 테스트
  - 데이터 저장/조회/수정/삭제 테스트

- **Controller Tests**:
  - API 엔드포인트 테스트
  - 요청/응답 검증
  - 인증/인가 테스트

### 8.3 테스트 코드 예시

```kotlin
// domain/model/command/TaskTest.kt
class TaskTest : FunSpec({
    test("회고가 작성된 할일은 수정할 수 없다") {
        val task = Task(
            id = TaskId(1),
            // ... 기타 필드
            retrospectiveId = RetrospectiveId(1)
        )

        shouldThrow<IllegalArgumentException> {
            task.updateTitle(TaskTitle("새로운 제목"), LocalDate.now())
        }
    }

    test("마감일이 회고 시점 이후인 경우 제한적으로 수정 가능") {
        val futureDueDate = LocalDate.now().plusDays(10)
        val task = Task(
            // ...
            period = TaskPeriod(LocalDate.now(), futureDueDate),
            retrospectiveId = RetrospectiveId(1)
        )

        shouldNotThrow<IllegalArgumentException> {
            task.updateTitle(TaskTitle("새로운 제목"), LocalDate.now())
        }
    }
})
```

## 9. 구현 우선순위

### Phase 1: 기본 CRUD (1주차)
1. Domain Layer 구현
   - Value Objects
   - Task Entity
   - Repository Interfaces
2. Infrastructure Layer 구현
   - Exposed Table 정의
   - Repository 구현체
3. Application Layer 구현
   - Command/Query 정의
   - Use Case 인터페이스
   - Service 구현체
4. Presentation Layer 구현
   - Request/Response DTOs
   - Controllers

### Phase 2: 주간 할일 기능 (2주차)
1. TaskDomainService 구현
2. GetWeeklyTasksUseCase 구현
3. 주간 통계 기능 구현
4. API 엔드포인트 추가

### Phase 3: 회고 연동 및 수정 제한 (3주차)
1. Retrospective Entity와의 연동
2. 수정 제한 로직 구현
3. 예외 처리 강화
4. 테스트 코드 작성

### Phase 4: 테스트 및 최적화 (4주차)
1. 단위 테스트 작성
2. 통합 테스트 작성
3. 성능 최적화
4. 코드 리뷰 및 리팩토링

## 10. 참고 사항

### 10.1 향후 확장 고려사항

- **댓글 기능**: Task에 댓글을 추가하는 기능
- **태그 기능**: Task에 태그를 붙여 분류하는 기능
- **알림 기능**: 마감일 임박 시 알림
- **반복 할일**: 주기적으로 반복되는 할일 설정
- **하위 할일**: 할일을 세부 작업으로 나누는 기능

### 10.2 성능 최적화 방안

- Redis 캐싱 활용 (주간 할일 조회)
- 인덱스 최적화 (user_id, start_date, due_date)
- N+1 문제 방지 (Exposed DSL 조인 쿼리)
- 페이징 처리 (대량 데이터 조회 시)

### 10.3 보안 고려사항

- 사용자 인증/인가 (JWT)
- 본인의 할일만 조회/수정/삭제 가능
- SQL Injection 방지 (Exposed DSL 사용)
- XSS 방지 (입력값 검증)

---

**작성일**: 2025-12-17
**작성자**: Claude Code
**버전**: 1.0
