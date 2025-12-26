package xyz.robinjoon.growweek.common.event

/**
 * 도메인 이벤트 발행 인터페이스
 */
interface DomainEventPublisher {
    /**
     * 이벤트를 발행합니다.
     * 동기/비동기 여부는 구현체에서 결정합니다.
     */
    fun <T> publish(event: DomainEvent<T>)

    /**
     * 페이로드만으로 이벤트를 발행합니다.
     * DefaultDomainEvent로 래핑됩니다.
     */
    fun <T> publish(payload: T) {
        publish(DefaultDomainEvent(payload = payload))
    }
}
