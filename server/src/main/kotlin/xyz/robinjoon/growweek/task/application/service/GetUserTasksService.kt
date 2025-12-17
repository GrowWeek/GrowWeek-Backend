package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.CursorPage
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery
import xyz.robinjoon.growweek.task.application.usecase.GetUserTasksUseCase
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class GetUserTasksService(
    private val taskRepository: TaskRepository
) : GetUserTasksUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: TaskApplicationQuery): Page<TaskDto> {
        // Application Query를 Domain Query로 변환
        val domainQuery = when (query) {
            is TaskApplicationQuery.CursorByUserId -> TaskQuery.CursorByUserId(
                userId = query.userId,
                pageInfo = query.pageInfo
            )
            is TaskApplicationQuery.OffsetByUserId -> TaskQuery.OffsetByUserId(
                userId = query.userId,
                pageInfo = query.pageInfo
            )
            else -> throw IllegalArgumentException("Unsupported query type for GetUserTasks: ${query::class.simpleName}")
        }

        // Repository를 통해 조회
        val page = taskRepository.findAll(domainQuery)

        // Domain 모델을 DTO로 변환
        return when (page) {
            is CursorPage -> CursorPage(
                items = page.items.map { TaskDto.from(it) },
                cursor = page.cursor,
                size = page.size,
                nextCursor = page.nextCursor,
                hasNext = page.hasNext
            )
            is OffsetPage -> OffsetPage(
                items = page.items.map { TaskDto.from(it) },
                page = page.page,
                size = page.size,
                totalPage = page.totalPage
            )
        }
    }
}
