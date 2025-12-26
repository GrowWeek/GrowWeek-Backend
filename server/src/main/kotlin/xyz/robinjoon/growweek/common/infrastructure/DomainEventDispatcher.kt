package xyz.robinjoon.growweek.common.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler

/**
 * 중앙 이벤트 디스패처
 *
 * 모든 DomainEvent를 수신하고, 등록된 핸들러들에게 분배합니다.
 * Java Type Erasure 문제를 우회하기 위해 supports() 메서드로 필터링합니다.
 */
@Component
class DomainEventDispatcher(
    private val handlers: List<DomainEventHandler<*>>
) {
    private val log = LoggerFactory.getLogger(DomainEventDispatcher::class.java)

    /**
     * 모든 DomainEvent를 수신하여 적절한 핸들러에게 분배
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun dispatch(event: DomainEvent<*>) {
        log.debug("Dispatching event: {}", event)

        handlers
            .filter { it.supports(event.payload::class) }
            .onEach { log.debug("Handler {} supports payload {}", it::class.simpleName, event.payload::class.simpleName) }
            .forEach {
                @Suppress("UNCHECKED_CAST")
                (it as DomainEventHandler<Any>).handle(event as DomainEvent<Any>)
            }
    }
}
