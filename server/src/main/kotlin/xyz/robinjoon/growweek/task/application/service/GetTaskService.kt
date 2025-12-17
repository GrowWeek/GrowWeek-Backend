package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery
import xyz.robinjoon.growweek.task.application.usecase.GetTaskUseCase
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class GetTaskService(
    private val taskRepository: TaskRepository
) : GetTaskUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: TaskApplicationQuery): TaskDto? {
        // Application Query를 Domain Query로 변환
        val domainQuery = when (query) {
            is TaskApplicationQuery.CursorByTaskId -> TaskQuery.CursorByTaskId(
                taskId = query.taskId,
                pageInfo = query.pageInfo
            )
            is TaskApplicationQuery.OffsetByTaskId -> TaskQuery.OffsetByTaskId(
                taskId = query.taskId,
                pageInfo = query.pageInfo
            )
            else -> throw IllegalArgumentException("Unsupported query type for GetTask: ${query::class.simpleName}")
        }

        // Repository를 통해 조회
        val page = taskRepository.findAll(domainQuery)
        val task = page.items.firstOrNull() ?: return null

        return TaskDto.from(task)
    }
}
