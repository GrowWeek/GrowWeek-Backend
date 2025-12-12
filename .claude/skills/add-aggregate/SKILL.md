---
name: add-aggregate
description: DDD의 Aggregate를 설계하고 구현합니다. 트랜잭션 일관성 경계를 정의하고 Aggregate Root를 식별할 때 사용하세요.
---

# Add Aggregate

## Instructions

### 1. Aggregate 특징

- **일관성 경계 (Consistency Boundary)**: 항상 일관된 상태 유지
- **트랜잭션 경계 (Transaction Boundary)**: 하나의 트랜잭션 단위
- **Aggregate Root**: 외부에서 접근 가능한 유일한 엔티티
- **불변 규칙 (Invariants)**: 반드시 지켜야 할 비즈니스 규칙

### 2. Aggregate Root 식별

다음 기준으로 Aggregate Root를 식별합니다:

1. **독립적인 생명주기**를 가지는가?
2. **다른 엔티티들을 조율**하는가?
3. **외부에서 접근**해야 하는가?
4. **불변 규칙을 관리**하는가?

### 3. Aggregate 경계 설정

**고려사항**:
- 트랜잭션 일관성 요구사항
- 변경 빈도
- 비즈니스 규칙
- 성능 (작게 유지)

**원칙**:
- Aggregate는 작게 유지
- 다른 Aggregate 참조는 ID로만
- 여러 Aggregate 변경 시 도메인 이벤트 사용

### 4. 구현 가이드

**Aggregate Root**:
- 모든 비즈니스 규칙 검증
- 내부 엔티티 접근 제어
- 팩토리 메서드 제공
- 도메인 이벤트 발행 (선택사항)

**내부 엔티티**:
- Aggregate Root를 통해서만 접근
- `private` 또는 `internal` 가시성

**Repository**:
- Aggregate Root 단위로 저장/조회
- 하나의 Aggregate = 하나의 Repository

### 5. 위치

`{bounded-context}/domain/model/command/`

## Examples

### 단순 Aggregate 예시

```kotlin
// domain/model/command/Task.kt
package xyz.robinjoon.growweek.task.domain.model.command

// Aggregate Root
data class Task private constructor(
    val id: TaskId,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val assigneeId: UserId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    init {
        require(title.isNotBlank()) { "Task title cannot be blank" }
        require(title.length <= 100) { "Task title is too long" }
    }

    // 비즈니스 로직
    fun complete(): Task {
        require(status != TaskStatus.COMPLETED) {
            "Task is already completed"
        }
        return copy(
            status = TaskStatus.COMPLETED,
            updatedAt = LocalDateTime.now()
        )
    }

    fun assignTo(userId: UserId): Task {
        return copy(
            assigneeId = userId,
            updatedAt = LocalDateTime.now()
        )
    }

    fun update(title: String, description: String): Task {
        require(title.isNotBlank()) { "Task title cannot be blank" }
        return copy(
            title = title,
            description = description,
            updatedAt = LocalDateTime.now()
        )
    }

    companion object {
        // 팩토리 메서드
        fun create(title: String, description: String): Task {
            return Task(
                id = TaskId.generate(),
                title = title,
                description = description,
                status = TaskStatus.PENDING,
                assigneeId = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}
```

### 복잡한 Aggregate 예시 (내부 엔티티 포함)

```kotlin
// domain/model/command/Order.kt
package xyz.robinjoon.growweek.order.domain.model.command

// Aggregate Root
data class Order private constructor(
    val id: OrderId,
    val customerId: CustomerId,
    private val _items: MutableList<OrderItem>,
    val status: OrderStatus,
    val totalAmount: Money,
    val createdAt: LocalDateTime
) {
    // 외부에는 읽기 전용으로 노출
    val items: List<OrderItem> get() = _items.toList()

    init {
        require(_items.isNotEmpty()) { "Order must have at least one item" }
        validateTotalAmount()
    }

    // 비즈니스 로직
    fun addItem(product: Product, quantity: Int): Order {
        require(status == OrderStatus.PENDING) {
            "Cannot add items to ${status.name} order"
        }
        require(quantity > 0) { "Quantity must be positive" }

        val existingItem = _items.find { it.productId == product.id }
        if (existingItem != null) {
            // 기존 아이템 수량 증가
            _items.remove(existingItem)
            _items.add(existingItem.increaseQuantity(quantity))
        } else {
            // 새 아이템 추가
            _items.add(OrderItem.create(product, quantity))
        }

        return recalculateTotalAmount()
    }

    fun removeItem(productId: ProductId): Order {
        require(status == OrderStatus.PENDING) {
            "Cannot remove items from ${status.name} order"
        }

        _items.removeIf { it.productId == productId }
        require(_items.isNotEmpty()) {
            "Order must have at least one item"
        }

        return recalculateTotalAmount()
    }

    fun confirm(): Order {
        require(status == OrderStatus.PENDING) {
            "Only PENDING orders can be confirmed"
        }
        return copy(status = OrderStatus.CONFIRMED)
    }

    fun cancel(): Order {
        require(status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
            "Cannot cancel ${status.name} order"
        }
        return copy(status = OrderStatus.CANCELLED)
    }

    private fun recalculateTotalAmount(): Order {
        val newTotal = _items.fold(Money.ZERO) { acc, item ->
            acc + item.totalPrice
        }
        return copy(totalAmount = newTotal)
    }

    private fun validateTotalAmount() {
        val calculatedTotal = _items.fold(Money.ZERO) { acc, item ->
            acc + item.totalPrice
        }
        require(totalAmount == calculatedTotal) {
            "Total amount mismatch: expected $calculatedTotal, got $totalAmount"
        }
    }

    companion object {
        fun create(customerId: CustomerId, items: List<OrderItem>): Order {
            require(items.isNotEmpty()) { "Order must have at least one item" }

            val totalAmount = items.fold(Money.ZERO) { acc, item ->
                acc + item.totalPrice
            }

            return Order(
                id = OrderId.generate(),
                customerId = customerId,
                _items = items.toMutableList(),
                status = OrderStatus.PENDING,
                totalAmount = totalAmount,
                createdAt = LocalDateTime.now()
            )
        }
    }
}

// 내부 엔티티 (Aggregate 외부에서 직접 접근 불가)
data class OrderItem internal constructor(
    val id: OrderItemId,
    val productId: ProductId,
    val productName: String,
    val unitPrice: Money,
    val quantity: Int
) {
    val totalPrice: Money get() = unitPrice * quantity

    init {
        require(quantity > 0) { "Quantity must be positive" }
    }

    internal fun increaseQuantity(amount: Int): OrderItem {
        return copy(quantity = quantity + amount)
    }

    internal companion object {
        fun create(product: Product, quantity: Int): OrderItem {
            return OrderItem(
                id = OrderItemId.generate(),
                productId = product.id,
                productName = product.name,
                unitPrice = product.price,
                quantity = quantity
            )
        }
    }
}

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
```

### Repository는 Aggregate Root만 저장

```kotlin
// domain/repository/OrderRepository.kt
package xyz.robinjoon.growweek.order.domain.repository

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: OrderId): Order?
    fun delete(id: OrderId)
    // OrderItem은 별도로 저장하지 않음 (Order와 함께 저장됨)
}
```

### 다른 Aggregate는 ID로만 참조

```kotlin
// Order Aggregate는 Customer Aggregate를 ID로만 참조
data class Order(
    val id: OrderId,
    val customerId: CustomerId,  // Customer 객체가 아닌 ID만 참조
    // ...
)

// Customer 정보가 필요하면 Application Service에서 조회
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository
) {
    fun createOrder(customerId: CustomerId, items: List<OrderItem>): OrderId {
        // Customer 존재 확인
        val customer = customerRepository.findById(customerId)
            ?: throw CustomerNotFoundException(customerId)

        // Order 생성 (Customer ID만 참조)
        val order = Order.create(customerId, items)
        return orderRepository.save(order).id
    }
}
```
