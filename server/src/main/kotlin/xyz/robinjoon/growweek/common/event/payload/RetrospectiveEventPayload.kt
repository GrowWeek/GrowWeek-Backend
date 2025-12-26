package xyz.robinjoon.growweek.common.event.payload

import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import java.time.LocalDate

/**
 * Retrospective 관련 이벤트 페이로드 (공유 계약)
 *
 * 발행측(retrospective)과 수신측(task) 모두 이 클래스를 사용합니다.
 * 다른 BC를 직접 참조하지 않고 common의 공유 계약을 통해 통신합니다.
 */
sealed interface RetrospectiveEventPayload {

    /**
     * 회고 완료 시 발행되는 이벤트
     */
    data class Completed(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : RetrospectiveEventPayload

    /**
     * 회고 삭제 시 발행되는 이벤트
     */
    data class Deleted(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveEventPayload
}
