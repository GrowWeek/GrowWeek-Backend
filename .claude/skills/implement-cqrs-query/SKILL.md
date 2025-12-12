---
name: implement-cqrs-query
description: 데이터를 조회하는 CQRS Query를 구현합니다. 읽기 작업을 최적화하고 Redis 캐싱, Exposed DSL 조인을 활용할 때 사용하세요.
---

# Implement CQRS Query

## Instructions

### 1. Query Flow

```
Request → Controller → Query UseCase → Domain Model (query/)
  → Repository (Exposed DSL + Redis) → PostgreSQL / Redis
```

### 2. Query 구성 요소

**1) Query DTO** (`application/query/`):
- 조회 조건 표현
- 불변 data class

**2) Query Handler** (`application/service/`):
- 조회 로직 처리
- `@Transactional(readOnly = true)`
- Redis 캐싱 적용

**3) Query Model** (`domain/model/query/`):
- 읽기에 최적화된 데이터 구조
- 여러 테이블 조인 결과
- 불변 data class

**4) Repository**:
- Exposed DSL로 최적화된 쿼리
- Redis 캐싱

### 3. 읽기 최적화 전략

**1) Redis 캐싱**:
- 자주 조회되는 데이터
- `@Cacheable`, `@CacheEvict` 사용
- TTL 설정

**2) Exposed DSL 조인 최적화**:
- N+1 문제 방지
- 필요한 컬럼만 조회 (Projection)
- `leftJoin`, `innerJoin` 적절히 사용

**3) 페이징**:
- 대량 데이터는 페이징 처리
- `limit`, `offset` 사용

**4) DTO 직접 매핑**:
- Entity 거치지 않고 DTO로 직접 매핑
- 불필요한 데이터 로드 방지

### 4. 구현 단계

1. Query DTO 정의
2. Query Model 정의 (읽기 전용)
3. Repository 인터페이스 정의
4. Repository 구현 (Exposed DSL + Redis)
5. Query Handler 구현 (`@Transactional(readOnly = true)`)
6. Controller 엔드포인트 추가

## Examples

### 단순 조회 Query

```kotlin
// 1. Query DTO
// application/query/FindTaskQuery.kt
package xyz.robinjoon.growweek.task.application.query

data class FindTaskQuery(
    val taskId: TaskId
)

// 2. Query Model
// domain/model/query/TaskDetail.kt
package xyz.robinjoon.growweek.task.domain.model.query

data class TaskDetail(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val assigneeName: String?,
    val createdAt: LocalDateTime
)

// 3. Repository Interface
// domain/repository/TaskQueryRepository.kt
package xyz.robinjoon.growweek.task.domain.repository

interface TaskQueryRepository {
    fun findDetailById(id: TaskId): TaskDetail?
}

// 4. Repository Implementation
// infrastructure/persistence/TaskQueryRepositoryImpl.kt
package xyz.robinjoon.growweek.task.infrastructure.persistence

@Repository
class TaskQueryRepositoryImpl : TaskQueryRepository {

    @Cacheable("task-details", key = "#id.value")
    override fun findDetailById(id: TaskId): TaskDetail? = transaction {
        (TaskTable leftJoin UserTable)
            .select { TaskTable.id eq id.value }
            .singleOrNull()
            ?.let { row ->
                TaskDetail(
                    id = row[TaskTable.id].value.toString(),
                    title = row[TaskTable.title],
                    description = row[TaskTable.description],
                    status = row[TaskTable.status],
                    assigneeName = row.getOrNull(UserTable.name),
                    createdAt = row[TaskTable.createdAt]
                )
            }
    }
}

// 5. Query Handler
// application/service/TaskQueryService.kt
package xyz.robinjoon.growweek.task.application.service

@Service
@Transactional(readOnly = true)
class TaskQueryService(
    private val taskQueryRepository: TaskQueryRepository
) {
    fun findTask(query: FindTaskQuery): TaskDetail? {
        return taskQueryRepository.findDetailById(query.taskId)
    }
}

// 6. Controller
// presentation/TaskController.kt
@GetMapping("/{id}")
fun getTask(@PathVariable id: Long): ResponseEntity<TaskDetailResponse> {
    val query = FindTaskQuery(TaskId(id))
    val task = taskQueryService.findTask(query)
        ?: return ResponseEntity.notFound().build()

    return ResponseEntity.ok(TaskDetailResponse.from(task))
}
```

### 검색 Query (페이징, 필터링)

```kotlin
// application/query/SearchTasksQuery.kt
data class SearchTasksQuery(
    val status: TaskStatus?,
    val assigneeId: String?,
    val keyword: String?,
    val page: Int = 0,
    val size: Int = 20
)

// domain/model/query/TaskSummary.kt
data class TaskSummary(
    val id: String,
    val title: String,
    val status: String,
    val assigneeName: String?,
    val createdAt: LocalDateTime
)

// Repository Implementation
@Repository
class TaskQueryRepositoryImpl : TaskQueryRepository {

    override fun search(
        status: TaskStatus?,
        assigneeId: UserId?,
        keyword: String?,
        pageable: Pageable
    ): Page<TaskSummary> = transaction {
        // 조건에 맞는 쿼리 생성
        val query = (TaskTable leftJoin UserTable)
            .selectAll()
            .apply {
                // 상태 필터
                status?.let {
                    andWhere { TaskTable.status eq it.name }
                }

                // 담당자 필터
                assigneeId?.let {
                    andWhere { TaskTable.assigneeId eq it.value }
                }

                // 키워드 검색
                keyword?.let {
                    andWhere {
                        (TaskTable.title like "%$it%") or
                        (TaskTable.description like "%$it%")
                    }
                }
            }

        // 전체 개수
        val total = query.count()

        // 페이징 결과
        val items = query
            .orderBy(TaskTable.createdAt to SortOrder.DESC)
            .limit(pageable.pageSize, pageable.offset)
            .map { row ->
                TaskSummary(
                    id = row[TaskTable.id].value.toString(),
                    title = row[TaskTable.title],
                    status = row[TaskTable.status],
                    assigneeName = row.getOrNull(UserTable.name),
                    createdAt = row[TaskTable.createdAt]
                )
            }

        PageImpl(items, pageable, total)
    }
}

// Query Handler with Caching
@Service
@Transactional(readOnly = true)
class TaskQueryService(
    private val taskQueryRepository: TaskQueryRepository
) {
    @Cacheable(
        value = ["task-search"],
        key = "#query.hashCode()"
    )
    fun searchTasks(query: SearchTasksQuery): Page<TaskSummary> {
        return taskQueryRepository.search(
            status = query.status,
            assigneeId = query.assigneeId?.let { UserId(it) },
            keyword = query.keyword,
            pageable = PageRequest.of(query.page, query.size)
        )
    }
}
```

### 복잡한 Join Query

```kotlin
// domain/model/query/TaskStatistics.kt
data class TaskStatistics(
    val totalTasks: Long,
    val completedTasks: Long,
    val pendingTasks: Long,
    val averageCompletionTime: Duration?,
    val tasksByAssignee: Map<String, Long>
)

// Repository Implementation
override fun getStatistics(): TaskStatistics = transaction {
    // 전체 작업 수
    val total = TaskTable.selectAll().count()

    // 완료된 작업 수
    val completed = TaskTable
        .select { TaskTable.status eq TaskStatus.COMPLETED.name }
        .count()

    // 대기 중인 작업 수
    val pending = TaskTable
        .select { TaskTable.status eq TaskStatus.PENDING.name }
        .count()

    // 담당자별 작업 수
    val byAssignee = (TaskTable innerJoin UserTable)
        .slice(UserTable.name, TaskTable.id.count())
        .selectAll()
        .groupBy(UserTable.name)
        .associate { row ->
            row[UserTable.name] to row[TaskTable.id.count()]
        }

    TaskStatistics(
        totalTasks = total,
        completedTasks = completed,
        pendingTasks = pending,
        averageCompletionTime = null, // 계산 로직 추가
        tasksByAssignee = byAssignee
    )
}
```

### Redis 캐싱 설정

```kotlin
// common/config/CacheConfig.kt
@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(
        redisConnectionFactory: RedisConnectionFactory
    ): CacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer()
                )
            )

        val cacheConfigurations = mapOf(
            "task-details" to config.entryTtl(Duration.ofMinutes(30)),
            "task-search" to config.entryTtl(Duration.ofMinutes(5)),
            "users" to config.entryTtl(Duration.ofHours(1))
        )

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
```

### 캐시 무효화

```kotlin
// Command 실행 시 캐시 무효화
@Service
@Transactional
class TaskCommandService(
    private val taskRepository: TaskRepository
) {
    @CacheEvict(
        value = ["task-details", "task-search"],
        allEntries = true
    )
    fun updateTask(taskId: TaskId, command: UpdateTaskCommand): Task {
        // Update logic
    }

    @CacheEvict(
        value = ["task-details"],
        key = "#taskId.value"
    )
    fun deleteTask(taskId: TaskId) {
        // Delete logic
    }
}
```
