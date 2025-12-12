---
name: add-value-object
description: 불변 Value Object를 생성합니다. 이메일, 금액, 날짜 범위 등 도메인 개념을 표현하는 타입 안전한 값 객체가 필요할 때 사용하세요.
---

# Add Value Object

## Instructions

### 1. Value Object 특징

- **불변성 (Immutable)**: 생성 후 변경 불가 (`val` 프로퍼티만 사용)
- **값 동등성 (Value Equality)**: 값으로만 비교 (data class 활용)
- **자가 검증 (Self-Validation)**: 생성 시 유효성 검증 (`init` 블록)
- **부작용 없음 (Side-effect Free)**: 순수 함수

### 2. 사용 시기

다음과 같은 경우 Value Object로 추상화합니다:

- **특정 형식을 가진 값**: 이메일, 전화번호, 주소
- **단위를 가진 값**: 금액, 수량, 거리
- **복합 값**: 날짜 범위, 기간, 좌표
- **도메인 규칙을 가진 값**: 비밀번호, 코드, 식별자

### 3. Kotlin 구현 방법

**방법 1: Inline Value Class (권장)**
- 런타임 오버헤드 없음
- 단일 프로퍼티에만 사용 가능

```kotlin
@JvmInline
value class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email" }
    }
}
```

**방법 2: Data Class**
- 여러 프로퍼티를 가진 복합 값
- 자동으로 `equals()`, `hashCode()`, `toString()` 생성

```kotlin
data class Money(val amount: Long, val currency: Currency) {
    init {
        require(amount >= 0) { "Amount must be non-negative" }
    }
}
```

### 4. 유효성 검증

- `init` 블록에서 검증
- `require()` 함수 사용
- 명확한 에러 메시지 제공
- 커스텀 예외 던지기 (선택사항)

### 5. 위치

- **도메인별 Value Object**: `{bounded-context}/domain/model/command/`
- **공통 Value Object**: `common/`

## Examples

### Inline Value Class 예시

```kotlin
// domain/model/command/Email.kt
package xyz.robinjoon.growweek.user.domain.model.command

@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(value.matches(EMAIL_REGEX)) {
            "Invalid email format: $value"
        }
    }

    companion object {
        private val EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
    }
}

// 사용 예시
val email = Email("user@example.com")
println(email.value) // "user@example.com"
```

### Data Class Value Object 예시

```kotlin
// common/Money.kt
package xyz.robinjoon.growweek.common

data class Money(
    val amount: Long,
    val currency: Currency = Currency.KRW
) {
    init {
        require(amount >= 0) {
            "Money amount cannot be negative: $amount"
        }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount - other.amount, currency)
    }

    operator fun times(multiplier: Int): Money {
        return Money(amount * multiplier, currency)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Cannot operate on different currencies: $currency vs ${other.currency}"
        }
    }

    companion object {
        val ZERO = Money(0)
    }
}

enum class Currency {
    KRW, USD, EUR
}
```

### 복합 Value Object 예시

```kotlin
// domain/model/command/DateRange.kt
package xyz.robinjoon.growweek.task.domain.model.command

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "End date cannot be before start date: $startDate ~ $endDate"
        }
    }

    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    fun getDays(): Long {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1
    }

    fun overlaps(other: DateRange): Boolean {
        return !endDate.isBefore(other.startDate) &&
               !other.endDate.isBefore(startDate)
    }
}
```

### ID Value Object 예시

```kotlin
// domain/model/command/TaskId.kt
package xyz.robinjoon.growweek.task.domain.model.command

@JvmInline
value class TaskId(val value: Long) {
    init {
        require(value > 0) { "TaskId must be positive: $value" }
    }

    companion object {
        fun generate(): TaskId {
            // ID 생성 로직 (실제로는 DB가 생성)
            return TaskId(System.currentTimeMillis())
        }

        fun from(value: String): TaskId {
            return TaskId(value.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid TaskId: $value"))
        }
    }
}
```

### 비즈니스 규칙을 가진 Value Object

```kotlin
// domain/model/command/Password.kt
package xyz.robinjoon.growweek.user.domain.model.command

@JvmInline
value class Password private constructor(val value: String) {
    companion object {
        private const val MIN_LENGTH = 8
        private val SPECIAL_CHARS = setOf('!', '@', '#', '$', '%', '^', '&', '*')

        fun create(rawPassword: String): Password {
            require(rawPassword.length >= MIN_LENGTH) {
                "Password must be at least $MIN_LENGTH characters"
            }
            require(rawPassword.any { it.isDigit() }) {
                "Password must contain at least one digit"
            }
            require(rawPassword.any { it.isUpperCase() }) {
                "Password must contain at least one uppercase letter"
            }
            require(rawPassword.any { it in SPECIAL_CHARS }) {
                "Password must contain at least one special character"
            }

            return Password(rawPassword)
        }

        fun fromEncoded(encoded: String): Password {
            // 이미 인코딩된 비밀번호 (검증 생략)
            return Password(encoded)
        }
    }
}
```
