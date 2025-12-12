---
name: add-domain-model
description: DDD 원칙에 따라 도메인 모델(Entity, Value Object)을 생성합니다. Entity와 Value Object를 구분하고 CQRS 패턴에 맞게 Command/Query 모델을 분리할 때 사용하세요.
---

# Add Domain Model

## Instructions

### 1. Entity vs Value Object 판단

**Entity를 사용하는 경우:**
- 고유 식별자(ID)가 있는 객체
- 생명주기가 있음 (생성, 수정, 삭제)
- 속성이 같아도 ID가 다르면 다른 객체

**Value Object를 사용하는 경우:**
- 식별자가 없는 불변 객체
- 값으로만 구분됨 (모든 속성이 같으면 같은 객체)
- 항상 불변(immutable)

### 2. Command vs Query 모델 분리

**Command Model** (`domain/model/command/`):
- 쓰기 작업에 최적화
- 비즈니스 로직 포함
- 유효성 검증 로직 포함
- Aggregate 단위로 설계

**Query Model** (`domain/model/query/`):
- 읽기 작업에 최적화
- 조회 전용 데이터 구조
- 여러 테이블 조인 결과를 담는 DTO 형태
- 불변 data class

### 3. Kotlin 구현 가이드

- `data class` 활용하여 불변성 유지
- `init` 블록에서 유효성 검증
- `require()` 함수로 비즈니스 규칙 강제
- 생성자는 private으로, 팩토리 메서드 제공 권장

### 4. 위치

- Entity: `{bounded-context}/domain/model/command/`
- Value Object: `{bounded-context}/domain/model/command/` 또는 `common/`
- Query Model: `{bounded-context}/domain/model/query/`

## Examples

### Entity 예시

```kotlin
// domain/model/command/Task.kt
package xyz.robinjoon.growweek.task.domain.model.command

data class Task private constructor(
    val id: TaskId,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val createdAt: LocalDateTime
) {
    init {
        require(title.isNotBlank()) { "Task title cannot be blank" }
        require(title.length <= 100) { "Task title must be 100 characters or less" }
    }

    fun complete(): Task = copy(status = TaskStatus.COMPLETED)

    companion object {
        fun create(title: String, description: String): Task {
            return Task(
                id = TaskId.generate(),
                title = title,
                description = description,
                status = TaskStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        }
    }
}
```

### Value Object 예시

```kotlin
// domain/model/command/Email.kt
package xyz.robinjoon.growweek.user.domain.model.command

@JvmInline
value class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format: $value" }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }
}
```

### Query Model 예시

```kotlin
// domain/model/query/TaskSummary.kt
package xyz.robinjoon.growweek.task.domain.model.query

data class TaskSummary(
    val id: String,
    val title: String,
    val status: String,
    val assigneeName: String?,
    val createdAt: LocalDateTime
)
```
