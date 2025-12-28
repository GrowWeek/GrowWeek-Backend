package xyz.robinjoon.growweek.task.application.command

import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.task.domain.model.*
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
        val startDate: LocalDate,
        val dueDate: LocalDate,
        val sensitivityLevel: SensitivityLevel = SensitivityLevel.NONE
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
        val sensitivityLevel: SensitivityLevel?
    ) : TaskApplicationCommand

    /**
     * 할일 상태 변경 커맨드
     */
    data class UpdateTaskStatus(
        val taskId: TaskId,
        val memberId: MemberId,
        val status: TaskStatus
    ) : TaskApplicationCommand

    /**
     * 할일 삭제 커맨드
     */
    data class DeleteTask(
        val taskId: TaskId,
        val memberId: MemberId
    ) : TaskApplicationCommand
}
