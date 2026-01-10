package xyz.robinjoon.growweek.task.presentation.rest.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery
import xyz.robinjoon.growweek.task.application.usecase.*
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import xyz.robinjoon.growweek.task.presentation.rest.request.CreateTaskRequest
import xyz.robinjoon.growweek.task.presentation.rest.request.UpdateTaskRequest
import xyz.robinjoon.growweek.task.presentation.rest.request.UpdateTaskStatusRequest
import xyz.robinjoon.growweek.task.presentation.rest.response.PageResponse
import xyz.robinjoon.growweek.task.presentation.rest.response.TaskResponse
import xyz.robinjoon.growweek.task.presentation.rest.response.WeeklyTaskResponse
import xyz.robinjoon.growweek.task.presentation.rest.response.toResponse
import java.time.LocalDate

/**
 * 할일 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getTaskUseCase: GetTaskUseCase,
    private val getWeeklyTasksUseCase: GetWeeklyTasksUseCase,
    private val getUserTasksUseCase: GetUserTasksUseCase,
) {
    /**
     * 새로운 할일을 생성한다.
     *
     * @param request 할일 생성 요청 정보
     * @param userId 사용자 식별자
     * @return 생성된 할일 정보
     */
    @PostMapping
    fun createTask(
        @RequestBody request: CreateTaskRequest,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<TaskResponse> {
        val command =
            TaskApplicationCommand.CreateTask(
                memberId = MemberId(userId),
                title = request.title,
                description = request.description,
                priority = request.priority,
                dueDate = LocalDate.parse(request.dueDate),
                sensitivityLevel =
                    request.sensitivityLevel?.let { SensitivityLevel.valueOf(it) }
                        ?: SensitivityLevel.NONE,
            )

        val taskDto = createTaskUseCase.execute(command)
        val response = TaskResponse.from(taskDto)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 할일 정보를 수정한다.
     *
     * @param taskId 할일 식별자
     * @param request 할일 수정 요청 정보
     * @param userId 사용자 식별자
     * @return 수정된 할일 정보
     */
    @PutMapping("/{taskId}")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskRequest,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<TaskResponse> {
        val command =
            TaskApplicationCommand.UpdateTask(
                taskId = TaskId(taskId),
                memberId = MemberId(userId),
                title = request.title,
                description = request.description,
                status = request.status?.let { TaskStatus.valueOf(it) },
                priority = request.priority,
                dueDate = request.dueDate?.let { LocalDate.parse(it) },
                sensitivityLevel = request.sensitivityLevel?.let { SensitivityLevel.valueOf(it) },
            )

        val taskDto = updateTaskUseCase.execute(command)
        val response = TaskResponse.from(taskDto)

        return ResponseEntity.ok(response)
    }

    /**
     * 할일 상태를 변경한다.
     *
     * @param taskId 할일 식별자
     * @param request 상태 변경 요청 정보
     * @param userId 사용자 식별자
     * @return 상태가 변경된 할일 정보
     */
    @PatchMapping("/{taskId}/status")
    fun updateTaskStatus(
        @PathVariable taskId: Long,
        @RequestBody request: UpdateTaskStatusRequest,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<TaskResponse> {
        val command =
            TaskApplicationCommand.UpdateTaskStatus(
                taskId = TaskId(taskId),
                memberId = MemberId(userId),
                status = TaskStatus.valueOf(request.status),
            )

        val taskDto = updateTaskStatusUseCase.execute(command)
        val response = TaskResponse.from(taskDto)

        return ResponseEntity.ok(response)
    }

    /**
     * 할일을 삭제한다.
     *
     * @param taskId 할일 식별자
     * @param userId 사용자 식별자
     * @return 204 No Content
     */
    @DeleteMapping("/{taskId}")
    fun deleteTask(
        @PathVariable taskId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<Void> {
        val command =
            TaskApplicationCommand.DeleteTask(
                taskId = TaskId(taskId),
                memberId = MemberId(userId),
            )

        deleteTaskUseCase.execute(command)

        return ResponseEntity.noContent().build()
    }

    /**
     * 할일 상세 정보를 조회한다.
     *
     * @param taskId 할일 식별자
     * @param userId 사용자 식별자
     * @return 할일 상세 정보, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{taskId}")
    fun getTask(
        @PathVariable taskId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<TaskResponse> {
        val query =
            TaskApplicationQuery.Offset.byTaskId(
                taskId = TaskId(taskId),
                memberId = MemberId(userId),
            )

        val taskDto =
            getTaskUseCase.execute(query)
                ?: return ResponseEntity.notFound().build()

        val response = TaskResponse.from(taskDto)

        return ResponseEntity.ok(response)
    }

    /**
     * 주간 할일 목록과 통계를 조회한다.
     *
     * @param weekId 조회할 주 식별자 (YYYY-Www 형식, 예: 2025-W02)
     * @param userId 사용자 식별자
     * @param page 페이지 번호 (오프셋 기반, 0부터 시작)
     * @param size 페이지 크기 (기본값: 20)
     * @param cursor 커서 (커서 기반 페이징 시 사용)
     * @return 주간 할일 목록과 통계
     */
    @GetMapping("/weekly")
    fun getWeeklyTasks(
        @RequestParam weekId: String,
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) cursor: String?,
    ): ResponseEntity<WeeklyTaskResponse> {
        val week = WeekId(weekId)

        val weeklyDto =
            if (cursor != null) {
                // Cursor 기반 페이징
                val query =
                    TaskApplicationQuery.Cursor.byMemberIdAndWeek(
                        memberId = MemberId(userId),
                        weekId = week,
                        cursor = cursor,
                        size = size ?: 20,
                    )
                getWeeklyTasksUseCase.execute(query)
            } else {
                // Offset 기반 페이징
                val query =
                    TaskApplicationQuery.Offset.byMemberIdAndWeek(
                        memberId = MemberId(userId),
                        weekId = week,
                        page = page ?: 0,
                        size = size ?: 20,
                    )
                getWeeklyTasksUseCase.execute(query)
            }

        val response = WeeklyTaskResponse.from(weeklyDto)

        return ResponseEntity.ok(response)
    }

    /**
     * 사용자의 전체 할일 목록을 조회한다.
     *
     * @param userId 사용자 식별자
     * @param page 페이지 번호 (오프셋 기반, 0부터 시작)
     * @param size 페이지 크기 (기본값: 20)
     * @param cursor 커서 (커서 기반 페이징 시 사용)
     * @return 할일 목록 (페이지네이션 적용)
     */
    @GetMapping
    fun getUserTasks(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) cursor: String?,
    ): ResponseEntity<PageResponse<TaskResponse>> {
        val pageDto =
            if (cursor != null) {
                // Cursor 기반 페이징
                val query =
                    TaskApplicationQuery.Cursor.byMemberId(
                        memberId = MemberId(userId),
                        cursor = cursor,
                        size = size ?: 20,
                    )
                getUserTasksUseCase.execute(query)
            } else {
                // Offset 기반 페이징
                val query =
                    TaskApplicationQuery.Offset.byMemberId(
                        memberId = MemberId(userId),
                        page = page ?: 0,
                        size = size ?: 20,
                    )
                getUserTasksUseCase.execute(query)
            }

        val response = pageDto.toResponse { TaskResponse.from(it) }

        return ResponseEntity.ok(response)
    }
}
