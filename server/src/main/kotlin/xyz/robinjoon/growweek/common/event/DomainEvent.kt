package xyz.robinjoon.growweek.common.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 모든 도메인 이벤트의 기반 인터페이스
 * @param T 이벤트 페이로드 타입
 */
interface DomainEvent<T> {
    val eventId: UUID
    val occurredAt: LocalDateTime
    val payload: T
}

/**
 * 기본 도메인 이벤트 구현
 */
data class DefaultDomainEvent<T>(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val payload: T
) : DomainEvent<T>
