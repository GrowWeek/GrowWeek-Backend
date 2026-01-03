package xyz.robinjoon.growweek.common.infrastructure

import org.springframework.context.ApplicationEventPublisher
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventPublisher

/**
 * Spring ApplicationEventPublisher를 사용한 구현체
 *
 * 위치: common/infrastructure (Spring 의존적인 구현체)
 */
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    override fun <T> publish(event: DomainEvent<T>) {
        applicationEventPublisher.publishEvent(event)
    }
}
