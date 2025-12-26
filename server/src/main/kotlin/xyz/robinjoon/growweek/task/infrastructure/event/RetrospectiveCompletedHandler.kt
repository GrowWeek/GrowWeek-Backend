package xyz.robinjoon.growweek.task.infrastructure.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

/**
 * 회고 완료 이벤트 핸들러
 *
 * 회고가 완료되면 해당 기간의 Task들에 retrospectiveId를 연결합니다.
 */
@Component
class RetrospectiveCompletedHandler(
    private val taskRepository: TaskRepository
) : DomainEventHandler<RetrospectiveEventPayload.Completed> {

    /**
     * 동기 처리: 같은 트랜잭션 내에서 실행
     * 비동기로 변경하려면 @Async + AFTER_COMMIT으로 변경
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleEvent(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        handle(event)
    }

    override fun handle(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        val payload = event.payload

        // 해당 기간의 Task 조회
        val query = TaskQuery.Offset.byUserIdAndWeek(
            userId = payload.userId,
            weekStart = payload.startDate,
            weekEnd = payload.endDate,
            size = Int.MAX_VALUE
        )
        val tasks = taskRepository.findAll(query).items

        // 각 Task에 회고 연결
        val linkCommands = tasks.map { task ->
            TaskCommand.LinkRetrospective(
                taskId = task.id,
                retrospectiveId = payload.retrospectiveId
            )
        }

        if (linkCommands.isNotEmpty()) {
            taskRepository.saveAll(linkCommands)
        }
    }

    override fun supports(payloadType: Class<*>): Boolean {
        return payloadType == RetrospectiveEventPayload.Completed::class.java
    }
}
