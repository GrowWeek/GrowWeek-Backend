---
name: implement-cqrs-command
description: 상태를 변경하는 CQRS Command를 구현합니다. 생성, 수정, 삭제 등의 쓰기 작업을 Command 패턴으로 구현할 때 사용하세요.
---

# Implement CQRS Command

## Instructions

### 1. Command Flow

```
Request DTO → Controller → Command UseCase → Domain Model (command/)
  → Repository (Exposed ORM) → PostgreSQL
```

### 2. Command 구성 요소

**1) Command DTO** (`application/command/`):
- 명령 의도 표현
- 불변 data class
- 유효성 검증은 Controller에서

**2) Command Handler** (`application/service/`):
- 명령 처리 로직
- `@Transactional` 애노테이션
- 도메인 객체 조율

**3) Domain Model** (`domain/model/command/`):
- 비즈니스 로직 수행
- 불변 규칙 검증

**4) Repository**:
- 인터페이스: `domain/repository/`
- 구현체: `infrastructure/persistence/` (Exposed ORM)

### 3. 구현 단계

1. Command DTO 정의
2. Domain Model 비즈니스 로직 구현
3. Repository 인터페이스 정의
4. Repository 구현 (Exposed ORM)
5. Command Handler 구현 (`@Transactional`)
6. Controller 엔드포인트 추가

### 4. 트랜잭션 관리

- Application Service 레벨에서 `@Transactional` 사용
- 실패 시 자동 롤백
- Exposed ORM의 `transaction` 블록과 조합

### 5. 유효성 검증

**요청 검증** (Controller/Application):
- `@Valid` 애노테이션
- Bean Validation (`@NotNull`, `@Size` 등)

**비즈니스 규칙 검증** (Domain):
- Domain Model 내부에서 수행
- `require()`, `check()` 사용
- 도메인 예외 발생

## Examples

### Create Command 구현

```kotlin
// 1. Command DTO
// application/command/CreateTaskCommand.kt
package xyz.robinjoon.growweek.task.application.command

data class CreateTaskCommand(
    val title: String,
    val description: String,
    val assigneeId: String? = null
)

// 2. Domain Model
// domain/model/command/Task.kt
package xyz.robinjoon.growweek.task.domain.model.command

data class Task private constructor(
    val id: TaskId,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val assigneeId: UserId?,
    val createdAt: LocalDateTime
) {
    init {
        require(title.isNotBlank()) { "Task title cannot be blank" }
        require(title.length <= 100) { "Task title is too long" }
    }

    companion object {
        fun create(title: String, description: String): Task {
            return Task(
                id = TaskId.generate(),
                title = title,
                description = description,
                status = TaskStatus.PENDING,
                assigneeId = null,
                createdAt = LocalDateTime.now()
            )
        }
    }

    fun assignTo(userId: UserId): Task {
        return copy(assigneeId = userId)
    }
}

// 3. Repository Interface
// domain/repository/TaskRepository.kt
package xyz.robinjoon.growweek.task.domain.repository

interface TaskRepository {
    fun save(task: Task): Task
    fun findById(id: TaskId): Task?
}

// 4. Repository Implementation (Exposed ORM)
// infrastructure/persistence/TaskRepositoryImpl.kt
package xyz.robinjoon.growweek.task.infrastructure.persistence

@Repository
class TaskRepositoryImpl : TaskRepository {
    override fun save(task: Task): Task = transaction {
        val id = TaskTable.insertAndGetId {
            it[title] = task.title
            it[description] = task.description
            it[status] = task.status.name
            it[assigneeId] = task.assigneeId?.value
            it[createdAt] = task.createdAt
        }

        task.copy(id = TaskId(id.value))
    }

    override fun findById(id: TaskId): Task? = transaction {
        TaskTable
            .select { TaskTable.id eq id.value }
            .singleOrNull()
            ?.let { toTask(it) }
    }

    private fun toTask(row: ResultRow): Task { /* ... */ }
}

// 5. Command Handler
// application/service/TaskCommandService.kt
package xyz.robinjoon.growweek.task.application.service

@Service
@Transactional
class TaskCommandService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    fun createTask(command: CreateTaskCommand): TaskId {
        // 도메인 객체 생성
        val task = Task.create(
            title = command.title,
            description = command.description
        )

        // 담당자 할당 (옵션)
        val updatedTask = command.assigneeId?.let { assigneeId ->
            val user = userRepository.findById(UserId(assigneeId))
                ?: throw UserNotFoundException(assigneeId)
            task.assignTo(user.id)
        } ?: task

        // 저장
        return taskRepository.save(updatedTask).id
    }
}

// 6. Controller Endpoint
// presentation/TaskController.kt
package xyz.robinjoon.growweek.task.presentation

@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(
    private val taskCommandService: TaskCommandService
) {
    @PostMapping
    fun createTask(
        @Valid @RequestBody request: CreateTaskRequest
    ): ResponseEntity<TaskResponse> {
        val taskId = taskCommandService.createTask(request.toCommand())
        val task = taskRepository.findById(taskId)!!

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(TaskResponse.from(task))
    }
}
```

### Update Command 구현

```kotlin
// application/command/UpdateTaskCommand.kt
data class UpdateTaskCommand(
    val title: String,
    val description: String
)

// Domain Model에 update 메서드 추가
fun update(title: String, description: String): Task {
    require(title.isNotBlank()) { "Task title cannot be blank" }
    require(title.length <= 100) { "Task title is too long" }

    return copy(
        title = title,
        description = description,
        updatedAt = LocalDateTime.now()
    )
}

// Command Handler
@Transactional
fun updateTask(taskId: TaskId, command: UpdateTaskCommand): Task {
    val task = taskRepository.findById(taskId)
        ?: throw TaskNotFoundException(taskId)

    val updatedTask = task.update(
        title = command.title,
        description = command.description
    )

    return taskRepository.save(updatedTask)
}
```

### Delete Command 구현

```kotlin
// application/command/DeleteTaskCommand.kt
data class DeleteTaskCommand(
    val taskId: TaskId
)

// Repository에 delete 메서드 추가
interface TaskRepository {
    fun delete(id: TaskId)
}

// Exposed ORM 구현
override fun delete(id: TaskId): Unit = transaction {
    TaskTable.deleteWhere { TaskTable.id eq id.value }
}

// Command Handler
@Transactional
fun deleteTask(command: DeleteTaskCommand) {
    val task = taskRepository.findById(command.taskId)
        ?: throw TaskNotFoundException(command.taskId)

    // 삭제 가능한지 비즈니스 규칙 검증
    require(task.status != TaskStatus.COMPLETED) {
        "Cannot delete completed task"
    }

    taskRepository.delete(command.taskId)
}
```

### 복잡한 Command (여러 Aggregate 조율)

```kotlin
// application/service/OrderCommandService.kt
@Service
@Transactional
class OrderCommandService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository
) {
    fun createOrder(command: CreateOrderCommand): OrderId {
        // 1. Customer 확인
        val customer = customerRepository.findById(command.customerId)
            ?: throw CustomerNotFoundException(command.customerId)

        // 2. Product 조회 및 재고 확인
        val orderItems = command.items.map { item ->
            val product = productRepository.findById(item.productId)
                ?: throw ProductNotFoundException(item.productId)

            require(product.stock >= item.quantity) {
                "Insufficient stock for product: ${product.name}"
            }

            OrderItem.create(product, item.quantity)
        }

        // 3. Order 생성
        val order = Order.create(customer.id, orderItems)

        // 4. 재고 차감
        command.items.forEach { item ->
            productRepository.decreaseStock(item.productId, item.quantity)
        }

        // 5. 저장
        return orderRepository.save(order).id
    }
}
```
