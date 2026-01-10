package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.usecase.CreateTaskUseCase
import xyz.robinjoon.growweek.task.domain.model.Priority
import xyz.robinjoon.growweek.task.domain.model.TaskDescription
import xyz.robinjoon.growweek.task.domain.model.TaskTitle
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.CompletedWeekQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedWeekRepository
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class CreateTaskService(
    private val taskRepository: TaskRepository,
    private val completedWeekRepository: CompletedWeekRepository,
) : CreateTaskUseCase {
    @Transactional
    override fun execute(command: TaskApplicationCommand.CreateTask): TaskDto {
        // startDate를 WeekId로 변환
        val weekId = WeekId.of(command.startDate)

        // 회고 완료된 주인지 검증
        validateNotInCompletedWeek(command, weekId)

        // Application Command를 Domain Command로 변환
        val domainCommand =
            TaskCommand.CreateTask(
                memberId = command.memberId,
                title = TaskTitle(command.title),
                description = command.description?.let { TaskDescription(it) },
                priority = Priority(command.priority),
                weekId = weekId,
                dueDate = command.dueDate,
                sensitivityLevel = command.sensitivityLevel,
            )

        // Repository를 통해 저장
        val savedTasks = taskRepository.saveAll(listOf(domainCommand))
        val createdTask = savedTasks.first()

        return TaskDto.from(createdTask)
    }

    /**
     * 할일이 회고 완료된 주에 속하는지 검증
     *
     * @throws IllegalArgumentException 회고가 완료된 주에 할일을 생성하려고 할 때
     */
    private fun validateNotInCompletedWeek(
        command: TaskApplicationCommand.CreateTask,
        weekId: WeekId,
    ) {
        val query =
            CompletedWeekQuery.Offset.byMemberIdAndWeekId(
                memberId = command.memberId,
                weekId = weekId,
            )

        val completedWeeks = completedWeekRepository.findAll(query).items

        if (completedWeeks.isNotEmpty()) {
            throw IllegalArgumentException(
                "회고가 완료된 기간(${weekId.startDate} ~ ${weekId.endDate})에는 할일을 추가할 수 없습니다.",
            )
        }
    }
}
