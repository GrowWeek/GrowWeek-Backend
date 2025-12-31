package xyz.robinjoon.growweek.task.infrastructure.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import kotlin.reflect.KClass

/**
 * 회고 완료 이벤트 핸들러
 *
 * 회고가 완료되면 해당 기간의 Task들에 retrospectiveId를 연결합니다.
 * DomainEventDispatcher에 의해 호출됩니다.
 */
@Component
class RetrospectiveCompletedHandler(
    private val taskRepository: TaskRepository
) : DomainEventHandler<RetrospectiveEventPayload.Completed> {

    private val log = LoggerFactory.getLogger(RetrospectiveCompletedHandler::class.java)

    override fun handle(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        log.info("Handling Retrospective Completed Event: {}", event)
        val payload = event.payload

        // 해당 기간의 Task 조회
        val query = TaskQuery.Offset.byMemberIdAndWeek(
            memberId = payload.memberId,
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

    override fun supports(payloadType: KClass<*>): Boolean {
        return payloadType == RetrospectiveEventPayload.Completed::class
    }
}
