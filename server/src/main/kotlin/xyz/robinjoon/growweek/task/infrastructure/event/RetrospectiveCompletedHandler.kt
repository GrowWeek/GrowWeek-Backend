package xyz.robinjoon.growweek.task.infrastructure.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.task.domain.model.command.CompletedRetrospectivePeriodCommand
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedRetrospectivePeriodRepository
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import kotlin.reflect.KClass

/**
 * 회고 완료 이벤트 핸들러
 *
 * 회고가 완료되면:
 * 1. 해당 기간의 Task들에 retrospectiveId를 연결합니다.
 * 2. 완료된 회고 기간 정보를 저장합니다. (Task 생성/수정 시 검증에 사용)
 *
 * DomainEventDispatcher에 의해 호출됩니다.
 */
@Component
class RetrospectiveCompletedHandler(
    private val taskRepository: TaskRepository,
    private val completedRetrospectivePeriodRepository: CompletedRetrospectivePeriodRepository,
) : DomainEventHandler<RetrospectiveEventPayload.Completed> {
    private val log = LoggerFactory.getLogger(RetrospectiveCompletedHandler::class.java)

    override fun handle(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        log.info("Handling Retrospective Completed Event: {}", event)
        val payload = event.payload

        // 1. 완료된 회고 기간 정보 저장
        saveCompletedRetrospectivePeriod(payload)

        // 2. 해당 기간의 Task 조회 및 회고 연결
        linkTasksToRetrospective(payload)
    }

    private fun saveCompletedRetrospectivePeriod(payload: RetrospectiveEventPayload.Completed) {
        val saveCommand =
            CompletedRetrospectivePeriodCommand.Save(
                retrospectiveId = payload.retrospectiveId,
                memberId = payload.memberId,
                startDate = payload.startDate,
                endDate = payload.endDate,
            )
        completedRetrospectivePeriodRepository.saveAll(listOf(saveCommand))
        log.info("Saved completed retrospective period: retrospectiveId={}", payload.retrospectiveId)
    }

    private fun linkTasksToRetrospective(payload: RetrospectiveEventPayload.Completed) {
        val query =
            TaskQuery.Offset.byMemberIdAndWeek(
                memberId = payload.memberId,
                weekStart = payload.startDate,
                weekEnd = payload.endDate,
                size = Int.MAX_VALUE,
            )
        val tasks = taskRepository.findAll(query).items

        val linkCommands =
            tasks.map { task ->
                TaskCommand.LinkRetrospective(
                    taskId = task.id,
                    retrospectiveId = payload.retrospectiveId,
                )
            }

        if (linkCommands.isNotEmpty()) {
            taskRepository.saveAll(linkCommands)
            log.info("Linked {} tasks to retrospective: retrospectiveId={}", linkCommands.size, payload.retrospectiveId)
        }
    }

    override fun supports(payloadType: KClass<*>): Boolean = payloadType == RetrospectiveEventPayload.Completed::class
}
