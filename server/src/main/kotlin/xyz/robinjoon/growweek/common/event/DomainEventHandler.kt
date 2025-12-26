package xyz.robinjoon.growweek.common.event

import kotlin.reflect.KClass

/**
 * 도메인 이벤트 핸들러 인터페이스
 * @param T 처리할 이벤트 페이로드 타입
 */
interface DomainEventHandler<T> {
    /**
     * 이벤트를 처리합니다.
     */
    fun handle(event: DomainEvent<T>)

    /**
     * 이 핸들러가 처리할 수 있는 이벤트 타입
     */
    fun supports(payloadType: KClass<*>): Boolean
}
