package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val taskRepository: TaskRepository
) : GetWeeklyTasksUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: TaskApplicationQuery.OffsetByUserIdAndWeek): WeeklyTaskDto {
        // Application Query를 Domain Query로 변환
        val domainQuery = TaskQuery.OffsetByUserIdAndWeek(
            userId = query.userId,
            weekStart = query.weekStart,
            weekEnd = query.weekEnd,
            pageInfo = query.pageInfo
        )

        return executeInternal(domainQuery, query.weekStart, query.weekEnd)
    }

    @Transactional(readOnly = true)
    override fun execute(query: TaskApplicationQuery.CursorByUserIdAndWeek): WeeklyTaskDto {
        // Application Query를 Domain Query로 변환
        val domainQuery = TaskQuery.CursorByUserIdAndWeek(
            userId = query.userId,
            weekStart = query.weekStart,
            weekEnd = query.weekEnd,
            pageInfo = query.pageInfo
        )

        return executeInternal(domainQuery, query.weekStart, query.weekEnd)
    }

    private fun executeInternal(
        domainQuery: TaskQuery,
        weekStart: java.time.LocalDate,
        weekEnd: java.time.LocalDate
    ): WeeklyTaskDto {
        // Repository를 통해 조회
        val page = taskRepository.findAll(domainQuery)
        val tasks = page.items.map { TaskDto.from(it) }

        // 통계 계산
        val statistics = calculateStatistics(tasks)

        return WeeklyTaskDto(
            weekStart = weekStart,
            weekEnd = weekEnd,
            tasks = tasks,
            statistics = statistics
        )
    }

    private fun calculateStatistics(tasks: List<TaskDto>): TaskStatisticsDto {
        val statusCount = tasks.groupingBy { it.status }.eachCount()
        return TaskStatisticsDto(
            total = tasks.size,
            todo = statusCount[TaskStatus.TODO] ?: 0,
            inProgress = statusCount[TaskStatus.IN_PROGRESS] ?: 0,
            done = statusCount[TaskStatus.DONE] ?: 0,
            cancel = statusCount[TaskStatus.CANCEL] ?: 0
        )
    }
}
