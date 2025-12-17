package xyz.robinjoon.growweek.task.presentation.rest.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery
import xyz.robinjoon.growweek.task.application.usecase.*
import xyz.robinjoon.growweek.task.domain.model.SensitivityLevel
import xyz.robinjoon.growweek.task.domain.model.TaskId
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import xyz.robinjoon.growweek.task.presentation.rest.request.CreateTaskRequest
import xyz.robinjoon.growweek.task.presentation.rest.request.UpdateTaskRequest
import xyz.robinjoon.growweek.task.presentation.rest.request.UpdateTaskStatusRequest
import xyz.robinjoon.growweek.task.presentation.rest.response.PageResponse
import xyz.robinjoon.growweek.task.presentation.rest.response.TaskResponse
import xyz.robinjoon.growweek.task.presentation.rest.response.WeeklyTaskResponse
import xyz.robinjoon.growweek.task.presentation.rest.response.toResponse
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getTaskUseCase: GetTaskUseCase,
    private val getWeeklyTasksUseCase: GetWeeklyTasksUseCase,
    private val getUserTasksUseCase: GetUserTasksUseCase
) {

    @PostMapping
    fun createTask(
        @RequestBody request: CreateTaskRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<TaskResponse> {
        val command = TaskApplicationCommand.CreateTask(
            userId = UserId(userId),
            title = request.title,
            description = request.description,
            priority = request.priority,
            startDate = LocalDate.parse(request.startDate),
            dueDate = LocalDate.parse(request.dueDate),
            sensitivityLevel = request.sensitivityLevel?.let { SensitivityLevel.valueOf(it) }
                ?: SensitivityLevel.NONE
        )

        val taskDto = createTaskUseCase.execute(command)
        val response = TaskResponse.from(taskDto)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{taskId}")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<TaskResponse> {
        val command = TaskApplicationCommand.UpdateTask(
            taskId = TaskId(taskId),
            userId = UserId(userId),
            title = request.title,
            description = request.description,
            status = request.status?.let { TaskStatus.valueOf(it) },
            priority = request.priority,
            dueDate = request.dueDate?.let { LocalDate.parse(it) },
            sensitivityLevel = request.sensitivityLevel?.let { SensitivityLevel.valueOf(it) }
        )

        val taskDto = updateTaskUseCase.execute(command)
        val response = TaskResponse.from(taskDto)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{taskId}/status")
    fun updateTaskStatus(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskStatusRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<TaskResponse> {
        val command = TaskApplicationCommand.UpdateTaskStatus(
            taskId = TaskId(taskId),
            userId = UserId(userId),
            status = TaskStatus.valueOf(request.status)
        )

        val taskDto = updateTaskStatusUseCase.execute(command)
        val response = TaskResponse.from(taskDto)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{taskId}")
    fun deleteTask(
        @PathVariable taskId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<Void> {
        val command = TaskApplicationCommand.DeleteTask(
            taskId = TaskId(taskId),
            userId = UserId(userId)
        )

        deleteTaskUseCase.execute(command)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{taskId}")
    fun getTask(
        @PathVariable taskId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<TaskResponse> {
        val query = TaskApplicationQuery.Offset.byTaskId(
            taskId = TaskId(taskId),
            userId = UserId(userId)
        )

        val taskDto = getTaskUseCase.execute(query)
            ?: return ResponseEntity.notFound().build()

        val response = TaskResponse.from(taskDto)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/weekly")
    fun getWeeklyTasks(
        @RequestParam weekStart: String,
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<WeeklyTaskResponse> {
        val startDate = LocalDate.parse(weekStart)
        val endDate = startDate.plusDays(6)

        val weeklyDto = if (cursor != null) {
            // Cursor 기반 페이징
            val query = TaskApplicationQuery.Cursor.byUserIdAndWeek(
                userId = UserId(userId),
                weekStart = startDate,
                weekEnd = endDate,
                cursor = cursor,
                size = size ?: 20
            )
            getWeeklyTasksUseCase.execute(query)
        } else {
            // Offset 기반 페이징
            val query = TaskApplicationQuery.Offset.byUserIdAndWeek(
                userId = UserId(userId),
                weekStart = startDate,
                weekEnd = endDate,
                page = page ?: 0,
                size = size ?: 20
            )
            getWeeklyTasksUseCase.execute(query)
        }

        val response = WeeklyTaskResponse.from(weeklyDto)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getUserTasks(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<PageResponse<TaskResponse>> {
        val pageDto = if (cursor != null) {
            // Cursor 기반 페이징
            val query = TaskApplicationQuery.Cursor.byUserId(
                userId = UserId(userId),
                cursor = cursor,
                size = size ?: 20
            )
            getUserTasksUseCase.execute(query)
        } else {
            // Offset 기반 페이징
            val query = TaskApplicationQuery.Offset.byUserId(
                userId = UserId(userId),
                page = page ?: 0,
                size = size ?: 20
            )
            getUserTasksUseCase.execute(query)
        }

        val response = pageDto.toResponse { TaskResponse.from(it) }

        return ResponseEntity.ok(response)
    }
}
