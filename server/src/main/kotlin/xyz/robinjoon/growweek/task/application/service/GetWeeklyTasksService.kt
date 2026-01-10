package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.dto.TaskStatisticsDto
import xyz.robinjoon.growweek.task.application.dto.WeeklyTaskDto
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery
import xyz.robinjoon.growweek.task.application.usecase.GetWeeklyTasksUseCase
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class GetWeeklyTasksService(
    private val taskRepository: TaskRepository,
) : GetWeeklyTasksUseCase {
    @Transactional(readOnly = true)
    override fun execute(query: TaskApplicationQuery.OffsetByMemberIdAndWeek): WeeklyTaskDto {
        // weekStart를 WeekId로 변환
        val weekId = WeekId.of(query.weekStart)

        // Application Query를 Domain Query로 변환
        val domainQuery =
            TaskQuery.OffsetByMemberIdAndWeek(
                memberId = query.memberId,
                weekId = weekId,
                pageInfo = query.pageInfo,
            )

        return executeInternal(domainQuery, weekId)
    }

    @Transactional(readOnly = true)
    override fun execute(query: TaskApplicationQuery.CursorByMemberIdAndWeek): WeeklyTaskDto {
        // weekStart를 WeekId로 변환
        val weekId = WeekId.of(query.weekStart)

        // Application Query를 Domain Query로 변환
        val domainQuery =
            TaskQuery.CursorByMemberIdAndWeek(
                memberId = query.memberId,
                weekId = weekId,
                pageInfo = query.pageInfo,
            )

        return executeInternal(domainQuery, weekId)
    }

    private fun executeInternal(
        domainQuery: TaskQuery,
        weekId: WeekId,
    ): WeeklyTaskDto {
        // Repository를 통해 조회
        val page = taskRepository.findAll(domainQuery)
        val tasks = page.items.map { TaskDto.from(it) }

        // 통계 계산
        val statistics = calculateStatistics(tasks)

        return WeeklyTaskDto(
            weekStart = weekId.startDate,
            weekEnd = weekId.endDate,
            tasks = tasks,
            statistics = statistics,
        )
    }

    private fun calculateStatistics(tasks: List<TaskDto>): TaskStatisticsDto {
        val statusCount = tasks.groupingBy { it.status }.eachCount()
        return TaskStatisticsDto(
            total = tasks.size,
            todo = statusCount[TaskStatus.TODO] ?: 0,
            inProgress = statusCount[TaskStatus.IN_PROGRESS] ?: 0,
            done = statusCount[TaskStatus.DONE] ?: 0,
            cancel = statusCount[TaskStatus.CANCEL] ?: 0,
        )
    }
}
