package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.usecase.CreateTaskUseCase
import xyz.robinjoon.growweek.task.domain.model.Priority
import xyz.robinjoon.growweek.task.domain.model.TaskDescription
import xyz.robinjoon.growweek.task.domain.model.TaskPeriod
import xyz.robinjoon.growweek.task.domain.model.TaskTitle
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.CompletedRetrospectivePeriodQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedRetrospectivePeriodRepository
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class CreateTaskService(
    private val taskRepository: TaskRepository,
    private val completedRetrospectivePeriodRepository: CompletedRetrospectivePeriodRepository,
) : CreateTaskUseCase {
    @Transactional
    override fun execute(command: TaskApplicationCommand.CreateTask): TaskDto {
        // 회고 완료된 기간과 겹치는지 검증
        validateNotInCompletedRetrospectivePeriod(command)

        // Application Command를 Domain Command로 변환
        val domainCommand =
            TaskCommand.CreateTask(
                memberId = command.memberId,
                title = TaskTitle(command.title),
                description = command.description?.let { TaskDescription(it) },
                priority = Priority(command.priority),
                period = TaskPeriod(command.startDate, command.dueDate),
                sensitivityLevel = command.sensitivityLevel,
            )

        // Repository를 통해 저장
        val savedTasks = taskRepository.saveAll(listOf(domainCommand))
        val createdTask = savedTasks.first()

        return TaskDto.from(createdTask)
    }

    /**
     * 할일 기간이 완료된 회고 기간과 겹치는지 검증
     *
     * @throws IllegalArgumentException 회고가 완료된 기간에 할일을 생성하려고 할 때
     */
    private fun validateNotInCompletedRetrospectivePeriod(command: TaskApplicationCommand.CreateTask) {
        val query =
            CompletedRetrospectivePeriodQuery.Offset.byMemberIdAndOverlappingPeriod(
                memberId = command.memberId,
                periodStart = command.startDate,
                periodEnd = command.dueDate,
            )

        val overlappingPeriods = completedRetrospectivePeriodRepository.findAll(query).items

        if (overlappingPeriods.isNotEmpty()) {
            val period = overlappingPeriods.first()
            throw IllegalArgumentException(
                "회고가 완료된 기간(${period.startDate} ~ ${period.endDate})에는 할일을 추가할 수 없습니다.",
            )
        }
    }
}
