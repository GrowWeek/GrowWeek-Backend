package xyz.robinjoon.growweek.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.CursorPage
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.task.domain.model.*
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import java.time.LocalDateTime

@Repository
class ExposedTaskRepository : TaskRepository {

    @Transactional
    override fun saveAll(commands: List<TaskCommand>): List<Task> {
        val savedTasks = mutableListOf<Task>()

        commands.forEach { command ->
            when (command) {
                is TaskCommand.CreateTask -> {
                    val insertedId = TaskTable.insert {
                        it[userId] = command.userId.value
                        it[title] = command.title.value
                        it[description] = command.description?.value
                        it[status] = TaskStatus.TODO.name
                        it[sensitivityLevel] = command.sensitivityLevel.name
                        it[priority] = command.priority.value
                        it[startDate] = command.period.startDate
                        it[dueDate] = command.period.dueDate
                        it[createdAt] = LocalDateTime.now()
                        it[updatedAt] = LocalDateTime.now()
                    } get TaskTable.id

                    // 생성된 Task 조회
                    val createdTask = TaskTable.selectAll().where { TaskTable.id eq insertedId }
                        .map { it.toTask() }
                        .single()
                    savedTasks.add(createdTask)
                }

                is TaskCommand.UpdateTask -> {
                    // 기존 Task 조회
                    val existingTask = TaskTable.selectAll().where {
                        (TaskTable.id eq command.taskId.value) and (TaskTable.userId eq command.userId.value)
                    }.map { it.toTask() }.singleOrNull()
                        ?: throw IllegalArgumentException("Task not found: ${command.taskId.value}")

                    // 회고 날짜 확인 (회고가 있으면)
                    val retrospectiveDate = existingTask.retrospectiveId?.let {
                        // TODO: 실제로는 Retrospective를 조회해서 날짜를 가져와야 함
                        null
                    }

                    // 업데이트 적용
                    var updatedTask = existingTask
                    command.title?.let { updatedTask = updatedTask.updateTitle(it, retrospectiveDate) }
                    command.description?.let { updatedTask = updatedTask.updateDescription(it, retrospectiveDate) }
                    command.status?.let { updatedTask = updatedTask.updateStatus(it, retrospectiveDate) }
                    command.priority?.let { updatedTask = updatedTask.updatePriority(it, retrospectiveDate) }
                    command.sensitivityLevel?.let { updatedTask = updatedTask.updateSensitivityLevel(it, retrospectiveDate) }
                    command.dueDate?.let { updatedTask = updatedTask.updateDueDate(it, retrospectiveDate) }

                    // DB 업데이트
                    TaskTable.update({ TaskTable.id eq command.taskId.value }) {
                        it[title] = updatedTask.title.value
                        it[description] = updatedTask.description?.value
                        it[status] = updatedTask.status.name
                        it[priority] = updatedTask.priority.value
                        it[sensitivityLevel] = updatedTask.sensitivityLevel.name
                        it[dueDate] = updatedTask.period.dueDate
                        it[updatedAt] = LocalDateTime.now()
                    }

                    savedTasks.add(updatedTask.copy(updatedAt = LocalDateTime.now()))
                }

                is TaskCommand.UpdateTaskStatus -> {
                    // 기존 Task 조회
                    val existingTask = TaskTable.selectAll().where {
                        (TaskTable.id eq command.taskId.value) and
                                (TaskTable.userId eq command.userId.value)
                    }.map { it.toTask() }.singleOrNull()
                        ?: throw IllegalArgumentException("Task not found: ${command.taskId.value}")

                    val retrospectiveDate = existingTask.retrospectiveId?.let { null }
                    val updatedTask = existingTask.updateStatus(command.status, retrospectiveDate)

                    // DB 업데이트
                    TaskTable.update({ TaskTable.id eq command.taskId.value }) {
                        it[status] = command.status.name
                        it[updatedAt] = LocalDateTime.now()
                    }

                    savedTasks.add(updatedTask.copy(updatedAt = LocalDateTime.now()))
                }

                is TaskCommand.DeleteTask -> {
                    // 물리 삭제
                    TaskTable.deleteWhere {
                        (TaskTable.id eq command.taskId.value) and
                                (TaskTable.userId eq command.userId.value)
                    }
                    // 삭제된 경우 반환할 Task 없음
                }

                is TaskCommand.LinkRetrospective -> {
                    // 회고 연결
                    TaskTable.update({ TaskTable.id eq command.taskId.value }) {
                        it[retrospectiveId] = command.retrospectiveId.value
                        it[updatedAt] = LocalDateTime.now()
                    }

                    val updatedTask = TaskTable.selectAll().where { TaskTable.id eq command.taskId.value }
                        .map { it.toTask() }
                        .singleOrNull()
                        ?: throw IllegalArgumentException("Task not found: ${command.taskId.value}")

                    savedTasks.add(updatedTask)
                }
            }
        }

        return savedTasks
    }

    @Transactional(readOnly = true)
    override fun findAll(query: TaskQuery): Page<Task> {
        return when (query.pageInfo) {
            is xyz.robinjoon.growweek.common.CursorPageInfo -> findWithCursor(query)
            is xyz.robinjoon.growweek.common.OffsetPageInfo -> findWithOffset(query)
        }
    }

    private fun findWithCursor(query: TaskQuery): CursorPage<Task> {
        val pageInfo = query.pageInfo as xyz.robinjoon.growweek.common.CursorPageInfo
        var baseQuery = TaskTable.selectAll()

        // 쿼리 타입별 필터 적용
        when (query) {
            is TaskQuery.CursorByUserId -> {
                baseQuery = baseQuery.andWhere { TaskTable.userId eq query.userId.value }
            }
            is TaskQuery.CursorByUserIdAndWeek -> {
                baseQuery = baseQuery.andWhere {
                    (TaskTable.userId eq query.userId.value) and
                            (TaskTable.startDate lessEq query.weekEnd) and
                            (TaskTable.dueDate greaterEq query.weekStart)
                }
            }
            is TaskQuery.CursorByTaskId -> {
                baseQuery = baseQuery.andWhere { TaskTable.id eq query.taskId.value }
            }
            else -> {}
        }

        // 커서 적용
        pageInfo.cursor?.let { cursor ->
            val cursorId = cursor.toLongOrNull()
            if (cursorId != null) {
                baseQuery = baseQuery.andWhere { TaskTable.id less cursorId }
            }
        }

        // 정렬
        baseQuery = when (pageInfo.orderBy) {
            "createdAt" -> baseQuery.orderBy(TaskTable.createdAt to SortOrder.DESC)
            "updatedAt" -> baseQuery.orderBy(TaskTable.updatedAt to SortOrder.DESC)
            "priority" -> baseQuery.orderBy(TaskTable.priority to SortOrder.DESC, TaskTable.dueDate to SortOrder.ASC)
            else -> baseQuery.orderBy(TaskTable.id to SortOrder.DESC)
        }

        // 페이징 (size + 1 조회하여 다음 페이지 존재 여부 확인)
        val items = baseQuery
            .limit(pageInfo.size + 1)
            .map { it.toTask() }

        val hasNext = items.size > pageInfo.size
        val resultItems = if (hasNext) items.dropLast(1) else items
        val nextCursor = if (hasNext) resultItems.lastOrNull()?.id?.value?.toString() else null

        return CursorPage(
            items = resultItems,
            cursor = pageInfo.cursor,
            size = pageInfo.size,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }

    private fun findWithOffset(query: TaskQuery): OffsetPage<Task> {
        val pageInfo = query.pageInfo as xyz.robinjoon.growweek.common.OffsetPageInfo
        var baseQuery = TaskTable.selectAll()

        // 쿼리 타입별 필터 적용
        when (query) {
            is TaskQuery.OffsetByUserId -> {
                baseQuery = baseQuery.andWhere { TaskTable.userId eq query.userId.value }
            }
            is TaskQuery.OffsetByUserIdAndWeek -> {
                baseQuery = baseQuery.andWhere {
                    (TaskTable.userId eq query.userId.value) and
                            (TaskTable.startDate lessEq query.weekEnd) and
                            (TaskTable.dueDate greaterEq query.weekStart)
                }
            }
            is TaskQuery.OffsetByTaskId -> {
                baseQuery = baseQuery.andWhere { TaskTable.id eq query.taskId.value }
            }
            else -> {}
        }

        // 정렬
        baseQuery = when (pageInfo.orderBy) {
            "createdAt" -> baseQuery.orderBy(TaskTable.createdAt to SortOrder.DESC)
            "updatedAt" -> baseQuery.orderBy(TaskTable.updatedAt to SortOrder.DESC)
            "priority" -> baseQuery.orderBy(TaskTable.priority to SortOrder.DESC, TaskTable.dueDate to SortOrder.ASC)
            else -> baseQuery.orderBy(TaskTable.id to SortOrder.DESC)
        }

        // 전체 개수
        val totalCount = baseQuery.count().toInt()
        val totalPage = if (totalCount == 0) 0 else (totalCount - 1) / pageInfo.size + 1

        // 페이징
        val items = baseQuery
            .limit(pageInfo.size)
            .offset((pageInfo.page * pageInfo.size).toLong())
            .map { it.toTask() }

        return OffsetPage(
            items = items,
            page = pageInfo.page,
            size = pageInfo.size,
            totalPage = totalPage
        )
    }

    private fun ResultRow.toTask(): Task {
        return Task(
            id = TaskId(this[TaskTable.id].value),
            userId = xyz.robinjoon.growweek.common.domain.UserId(this[TaskTable.userId]),
            title = TaskTitle(this[TaskTable.title]),
            description = this[TaskTable.description]?.let { TaskDescription(it) },
            status = TaskStatus.valueOf(this[TaskTable.status]),
            sensitivityLevel = SensitivityLevel.valueOf(this[TaskTable.sensitivityLevel]),
            priority = Priority(this[TaskTable.priority]),
            period = TaskPeriod(this[TaskTable.startDate], this[TaskTable.dueDate]),
            createdAt = this[TaskTable.createdAt],
            updatedAt = this[TaskTable.updatedAt],
            retrospectiveId = this[TaskTable.retrospectiveId]?.let { RetrospectiveId(it) }
        )
    }
}
