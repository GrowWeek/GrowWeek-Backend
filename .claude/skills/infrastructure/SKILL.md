---
name: add-or-update-infrastructure
description: Bounded Context에 새로운 infrastructure 를 추가하거나 수정할 때 사용하세요.
---

# Add or Update Infrastructure

## Instructions

새로운 인프라스트럭처를 추가하거나 기존 인프라스트럭처를 수정할 때 다음 규칙을 따르세요:

### 1. infrastructure 디렉토리 구조

```
└── infrastructure/
    ├── persistence/
    ├── event/          # → 별도 skill(event) 참고
    ├── security/
    └── external/
```

> **참고**: `event/` 디렉토리의 상세 작성 규칙은 별도의 `event` skill에 정의되어 있습니다. 이벤트 핸들러를 추가하거나 수정할 때는 해당 skill을 사용하세요.

### 2. persistence 디렉토리

persistence 디렉토리에는 Exposed ORM을 사용한 데이터베이스 관련 domain/repository 인터페이스의 구현체가 위치합니다. 예를 들어 :

```kotlin

class ExposedTaskRepositoryImpl : TaskRepository {
    @Transactional
    override fun saveTasks(tasks: List<Task>): List<Task> {
        val savedTasks = mutableListOf<Task>()

        tasks.forEach { task ->
            if (task.id == null) {
                // INSERT
                val insertedId = TaskTable.insert {
                    it[memberId] = task.memberId.value
                    it[weekId] = task.weekId.value
                    it[title] = task.title.value
                    it[description] = task.description?.value
                    it[step] = task.step.name
                    it[isDelete] = task.state.isDelete
                    it[isLocked] = task.state.isLocked
                    it[isCarriedOver] = task.state.isCarriedOver
                    it[isSensitive] = task.state.isSensitive
                    it[createdAt] = task.auditInfo.createdAt
                    it[updatedAt] = task.auditInfo.updatedAt
                } get TaskTable.id

                savedTasks.add(task.copy(id = TaskId(insertedId.value)))
            } else {
                // UPDATE
                TaskTable.update({ TaskTable.id eq task.id.value }) {
                    it[memberId] = task.memberId.value
                    it[weekId] = task.weekId.value
                    it[title] = task.title.value
                    it[description] = task.description?.value
                    it[step] = task.step.name
                    it[isDelete] = task.state.isDelete
                    it[isLocked] = task.state.isLocked
                    it[isCarriedOver] = task.state.isCarriedOver
                    it[isSensitive] = task.state.isSensitive
                    it[updatedAt] = LocalDateTime.now()
                }
                savedTasks.add(task)
            }
        }

        return savedTasks
    }

    @Transactional(readOnly = true)
    override fun findTasks(query: TaskRepository.TaskPageQuery): Page<Task> {
        return when (query) {
            is TaskRepository.TaskOffsetPageQuery -> findTasksWithOffset(query)
            is TaskRepository.TaskCursorPageQuery -> findTasksWithCursor(query)
        }
    }

    private fun findTasksWithOffset(query: TaskRepository.TaskOffsetPageQuery): OffsetPage<Task> {
        var baseQuery = TaskTable.selectAll()
            .where { TaskTable.isDelete eq false }

        // 필터 적용
        if (query.taskIds.isNotEmpty()) {
            baseQuery = baseQuery.andWhere { TaskTable.id inList query.taskIds.map { it.value } }
        }
        query.weekId?.let { weekId ->
            baseQuery = baseQuery.andWhere { TaskTable.weekId eq weekId.value }
        }
        query.memberId?.let { memberId ->
            baseQuery = baseQuery.andWhere { TaskTable.memberId eq memberId.value }
        }

        // 정렬
        baseQuery = when (query.orderBy) {
            "createdAt" -> baseQuery.orderBy(TaskTable.createdAt to SortOrder.DESC)
            "updatedAt" -> baseQuery.orderBy(TaskTable.updatedAt to SortOrder.DESC)
            else -> baseQuery.orderBy(TaskTable.createdAt to SortOrder.DESC)
        }

        // 전체 개수
        val totalCount = baseQuery.count().toInt()
        val totalPage = totalCount / query.size + if (totalCount % query.size > 0) 1 else 0

        // 페이징
        val items = baseQuery
            .limit(query.size)
            .offset((query.page * query.size).toLong())
            .map { it.toTask() }

        return OffsetPage(
            items = items,
            page = query.page,
            size = query.size,
            totalPage = totalPage,
        )
    }

    private fun findTasksWithCursor(query: TaskRepository.TaskCursorPageQuery): CursorPage<Task> {
        var baseQuery = TaskTable.selectAll()
            .where { TaskTable.isDelete eq false }

        // 필터 적용
        if (query.taskIds.isNotEmpty()) {
            baseQuery = baseQuery.andWhere { TaskTable.id inList query.taskIds.map { it.value } }
        }
        query.weekId?.let { weekId ->
            baseQuery = baseQuery.andWhere { TaskTable.weekId eq weekId.value }
        }
        query.memberId?.let { memberId ->
            baseQuery = baseQuery.andWhere { TaskTable.memberId eq memberId.value }
        }

        // 커서 적용
        query.cursor?.let { cursor ->
            val cursorId = cursor.toLongOrNull()
            if (cursorId != null) {
                baseQuery = baseQuery.andWhere { TaskTable.id less cursorId }
            }
        }

        // 정렬
        baseQuery = when (query.orderBy) {
            "createdAt" -> baseQuery.orderBy(TaskTable.createdAt to SortOrder.DESC)
            "updatedAt" -> baseQuery.orderBy(TaskTable.updatedAt to SortOrder.DESC)
            else -> baseQuery.orderBy(TaskTable.id to SortOrder.DESC)
        }

        // 페이징 (size + 1 조회하여 다음 페이지 존재 여부 확인)
        val items = baseQuery
            .limit(query.size + 1)
            .map { it.toTask() }

        val hasNext = items.size > query.size
        val resultItems = if (hasNext) items.dropLast(1) else items
        val nextCursor = if (hasNext) resultItems.lastOrNull()?.id?.value?.toString() else null

        return CursorPage(
            items = resultItems,
            cursor = query.cursor,
            size = query.size,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }

    private fun ResultRow.toTask(): Task {
        return Task.load(
            id = TaskId(this[TaskTable.id].value),
            title = TaskTitle(this[TaskTable.title]),
            description = TaskDescription.orNull(this[TaskTable.description]),
            state = TaskState(
                isDelete = this[TaskTable.isDelete],
                isLocked = this[TaskTable.isLocked],
                isCarriedOver = this[TaskTable.isCarriedOver],
                isSensitive = this[TaskTable.isSensitive]
            ),
            step = TaskStep.valueOf(this[TaskTable.step]),
            memberId = MemberId(this[TaskTable.memberId]),
            weekId = WeekId(this[TaskTable.weekId]),
            auditInfo = AuditInfo(
                createdAt = this[TaskTable.createdAt],
                updatedAt = this[TaskTable.updatedAt]
            )
        )
    }
}

```
또한, Exposed 설정 파일이나 테이블 objects도 이 디렉토리에 위치합니다. 예를 들어 :

```kotlin
object TaskTable : LongIdTable("tasks") {
    // 기본 필드
    val memberId = long("member_id")
    val weekId = long("week_id")
    val title = varchar("title", 200)
    val description = text("description").nullable()

    // 상태
    val step = varchar("step", 20)

    // TaskState 플래그들
    val isDelete = bool("is_deleted").default(false)
    val isLocked = bool("is_locked").default(false)
    val isCarriedOver = bool("is_carried_over").default(false)
    val isSensitive = bool("is_sensitive").default(false)

    // 감사 정보
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at").nullable()

    init {
        // 복합 인덱스
        index(false, memberId, weekId)
        index(false, weekId)
        index(false, step)
        index(false, isDelete)
    }
}
```

### 3. external 디렉토리

external 디렉토리에는 OpenFeign 클라이언트를 사용한 외부 서비스 연동 관련 domain/repository 인터페이스의 구현체가 위치합니다. 기능상 필요하지 않다면 이 디렉토리 안에는 파일이 존재하지 않을 수 있습니다. 예를 들어 :

```kotlin
@FeignClient(name = "notificationService", url = "\${feign.notification-service.url}")
interface NotificationClient {
    @PostMapping("/api/v1/notifications")
    fun sendNotification(@RequestBody request: NotificationRequest): NotificationResponse
}
```
Open Feign 설정이나 관련 DTO들도 이 디렉토리에 위치합니다. 예를 들어 :
```kotlin
data class NotificationRequest(
    val userId: Long,
    val title: String,
    val message: String
)
data class NotificationResponse(
    val notificationId: Long,
    val status: String
)
```

만약, 서로 다른 외부 서비스 연동을 구분하기 위해 하위 디렉토리가 필요하다면, 해당 외부 서비스 이름으로 하위 디렉토리를 생성하여 관리할 수 있습니다. 예를 들어 :
```
└── infrastructure/
    ├── persistence/
    └── external/
        ├── kafka/
        └── aws/
```

### 4. security 디렉토리

security 디렉토리에는 Spring Security 관련 인프라 구현체가 위치합니다. 기능상 필요하지 않다면 이 디렉토리 안에는 파일이 존재하지 않을 수 있습니다. 예를 들어 JWT 인증 필터, 인증 토큰, ArgumentResolver 등이 이 디렉토리에 위치합니다.

### 5. 기타 경로 추가

위에 정의된 persistence, external, security 외에 새로운 하위 디렉토리가 필요한 경우 추가할 수 있습니다. 단, 새로운 경로를 추가할 경우 반드시 이 skills 문서에 해당 디렉토리의 역할과 규칙을 반영하고, 사용자에게 새로운 경로가 추가되었음을 분명히 응답해야 합니다.