package xyz.robinjoon.growweek.task.application.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import java.time.LocalDate

sealed interface TaskApplicationCommand {
    /**
     * 할일 생성 커맨드
     */
    data class CreateTask(
        val memberId: MemberId,
        val title: String,
        val description: String?,
        val priority: Int,
        val dueDate: LocalDate,
        val sensitivityLevel: SensitivityLevel = SensitivityLevel.NONE,
    ) : TaskApplicationCommand

    /**
     * 할일 수정 커맨드
     */
    data class UpdateTask(
        val taskId: TaskId,
        val memberId: MemberId,
        val title: String?,
        val description: String?,
        val status: TaskStatus?,
        val priority: Int?,
        val dueDate: LocalDate?,
        val sensitivityLevel: SensitivityLevel?,
    ) : TaskApplicationCommand

    /**
     * 할일 상태 변경 커맨드
     */
    data class UpdateTaskStatus(
        val taskId: TaskId,
        val memberId: MemberId,
        val status: TaskStatus,
    ) : TaskApplicationCommand

    /**
     * 할일 삭제 커맨드
     */
    data class DeleteTask(
        val taskId: TaskId,
        val memberId: MemberId,
    ) : TaskApplicationCommand

    companion object {
        fun createTask(
            memberId: Long,
            title: String,
            description: String?,
            priority: Int,
            dueDate: String,
            sensitivityLevel: String? = null,
        ): CreateTask =
            CreateTask(
                memberId = MemberId(memberId),
                title = title,
                description = description,
                priority = priority,
                dueDate = LocalDate.parse(dueDate),
                sensitivityLevel = parseSensitivityLevel(sensitivityLevel),
            )

        fun updateTask(
            taskId: Long,
            memberId: Long,
            title: String?,
            description: String?,
            status: String?,
            priority: Int?,
            dueDate: String?,
            sensitivityLevel: String?,
        ): UpdateTask =
            UpdateTask(
                taskId = TaskId(taskId),
                memberId = MemberId(memberId),
                title = title,
                description = description,
                status = status?.let(TaskStatus::valueOf),
                priority = priority,
                dueDate = dueDate?.let(LocalDate::parse),
                sensitivityLevel = sensitivityLevel?.let(SensitivityLevel::valueOf),
            )

        fun updateTaskStatus(
            taskId: Long,
            memberId: Long,
            status: String,
        ): UpdateTaskStatus =
            UpdateTaskStatus(
                taskId = TaskId(taskId),
                memberId = MemberId(memberId),
                status = TaskStatus.valueOf(status),
            )

        fun deleteTask(
            taskId: Long,
            memberId: Long,
        ): DeleteTask =
            DeleteTask(
                taskId = TaskId(taskId),
                memberId = MemberId(memberId),
            )

        private fun parseSensitivityLevel(level: String?): SensitivityLevel = level?.let(SensitivityLevel::valueOf) ?: SensitivityLevel.NONE
    }
}
