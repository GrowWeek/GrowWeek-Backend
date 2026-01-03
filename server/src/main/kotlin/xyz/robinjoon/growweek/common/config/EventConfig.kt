package xyz.robinjoon.growweek.common.config

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.robinjoon.growweek.common.event.DomainEventPublisher
import xyz.robinjoon.growweek.common.infrastructure.SpringDomainEventPublisher

@Configuration
class EventConfig {
    @Bean
    fun domainEventPublisher(applicationEventPublisher: ApplicationEventPublisher): DomainEventPublisher =
        SpringDomainEventPublisher(applicationEventPublisher)
}
