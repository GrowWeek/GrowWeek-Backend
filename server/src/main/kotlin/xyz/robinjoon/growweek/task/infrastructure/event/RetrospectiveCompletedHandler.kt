package xyz.robinjoon.growweek.task.infrastructure.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.common.event.DomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.task.domain.model.command.CompletedWeekCommand
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedWeekRepository
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
    private val completedWeekRepository: CompletedWeekRepository,
) : DomainEventHandler<RetrospectiveEventPayload.Completed> {
    private val log = LoggerFactory.getLogger(RetrospectiveCompletedHandler::class.java)

    override fun handle(event: DomainEvent<RetrospectiveEventPayload.Completed>) {
        log.info("Handling Retrospective Completed Event: {}", event)
        val payload = event.payload

        // startDate를 WeekId로 변환
        val weekId = WeekId.of(payload.startDate)

        // 1. 완료된 회고 주 정보 저장
        saveCompletedWeek(payload, weekId)

        // 2. 해당 주의 Task 조회 및 회고 연결
        linkTasksToRetrospective(payload, weekId)
    }

    private fun saveCompletedWeek(
        payload: RetrospectiveEventPayload.Completed,
        weekId: WeekId,
    ) {
        val saveCommand =
            CompletedWeekCommand.Save(
                retrospectiveId = payload.retrospectiveId,
                memberId = payload.memberId,
                weekId = weekId,
            )
        completedWeekRepository.saveAll(listOf(saveCommand))
        log.info("Saved completed week: retrospectiveId={}, weekId={}", payload.retrospectiveId, weekId.value)
    }

    private fun linkTasksToRetrospective(
        payload: RetrospectiveEventPayload.Completed,
        weekId: WeekId,
    ) {
        val query =
            TaskQuery.Offset.byMemberIdAndWeek(
                memberId = payload.memberId,
                weekId = weekId,
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
