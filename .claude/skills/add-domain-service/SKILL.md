---
name: add-domain-service
description: Entity나 Value Object로 표현하기 어려운 도메인 비즈니스 로직을 도메인 서비스로 구현합니다. 여러 Aggregate에 걸친 로직이나 상태 없는 도메인 연산이 필요할 때 사용하세요.
---

# Add Domain Service

## Instructions

### 1. Domain Service 사용 시기

다음 경우에 Domain Service를 사용합니다:

- **여러 Aggregate에 걸친 비즈니스 로직**: 단일 Entity/VO에 속하지 않는 로직
- **상태 없는 도메인 연산**: 특정 객체의 상태에 속하지 않는 계산
- **도메인 용어로 표현되는 행위**: "할인 정책 적용", "배송비 계산" 등

### 2. Domain Service vs Application Service

**Domain Service** (`domain/service/`):
- 순수한 비즈니스 로직
- 도메인 객체만 의존
- 외부 의존성 없음 (DB, API 등)
- 상태를 가지지 않음 (stateless)

**Application Service** (`application/service/`):
- Use Case 구현
- 트랜잭션 관리
- 도메인 객체 조율
- Infrastructure Layer 의존 가능

### 3. 구현 가이드

- 인터페이스로 추상화
- 구현체는 `impl` 패키지에 배치 가능
- 도메인 용어 사용 (기술 용어 지양)
- 상태를 가지지 않도록 설계

### 4. 위치

- 인터페이스: `{bounded-context}/domain/service/`
- 구현체: `{bounded-context}/domain/service/` (또는 `impl/`)

## Examples

### Domain Service 인터페이스

```kotlin
// domain/service/PriceCalculator.kt
package xyz.robinjoon.growweek.order.domain.service

interface PriceCalculator {
    /**
     * 주문 총액을 계산합니다.
     * 할인 정책, 배송비, 세금을 포함합니다.
     */
    fun calculateTotalPrice(
        order: Order,
        discountPolicy: DiscountPolicy
    ): Money
}
```

### Domain Service 구현

```kotlin
// domain/service/DefaultPriceCalculator.kt
package xyz.robinjoon.growweek.order.domain.service

class DefaultPriceCalculator : PriceCalculator {
    override fun calculateTotalPrice(
        order: Order,
        discountPolicy: DiscountPolicy
    ): Money {
        val itemsPrice = order.items.sumOf { it.price.value }
        val discount = discountPolicy.calculate(order)
        val shipping = calculateShipping(order)
        val tax = calculateTax(itemsPrice - discount.value)

        return Money(itemsPrice - discount.value + shipping.value + tax.value)
    }

    private fun calculateShipping(order: Order): Money {
        // 배송비 계산 로직
        return if (order.totalAmount() >= Money(50000)) {
            Money.ZERO
        } else {
            Money(3000)
        }
    }

    private fun calculateTax(amount: Long): Money {
        // 세금 계산 로직 (10%)
        return Money((amount * 0.1).toLong())
    }
}
```

### Application Service에서 사용

```kotlin
// application/service/OrderService.kt
package xyz.robinjoon.growweek.order.application.service

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val priceCalculator: PriceCalculator
) {
    fun createOrder(command: CreateOrderCommand): OrderId {
        val order = Order.create(command.items)
        val totalPrice = priceCalculator.calculateTotalPrice(
            order,
            command.discountPolicy
        )

        order.updateTotalPrice(totalPrice)
        return orderRepository.save(order).id
    }
}
```
