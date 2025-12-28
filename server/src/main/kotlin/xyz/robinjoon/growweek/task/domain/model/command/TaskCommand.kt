package xyz.robinjoon.growweek.task.domain.model.command

import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.task.domain.model.*
import java.time.LocalDate

sealed interface TaskCommand {
    /**
     * 할일 생성 커맨드
     */
    data class CreateTask(
        val memberId: MemberId,
        val title: TaskTitle,
        val description: TaskDescription?,
        val priority: Priority,
        val period: TaskPeriod,
        val sensitivityLevel: SensitivityLevel = SensitivityLevel.NONE
    ) : TaskCommand

    /**
     * 할일 수정 커맨드
     */
    data class UpdateTask(
        val taskId: TaskId,
        val memberId: MemberId,
        val title: TaskTitle?,
        val description: TaskDescription?,
        val status: TaskStatus?,
        val priority: Priority?,
        val dueDate: LocalDate?,
        val sensitivityLevel: SensitivityLevel?
    ) : TaskCommand

    /**
     * 할일 상태 변경 커맨드
     */
    data class UpdateTaskStatus(
        val taskId: TaskId,
        val memberId: MemberId,
        val status: TaskStatus
    ) : TaskCommand

    /**
     * 할일 삭제 커맨드
     */
    data class DeleteTask(
        val taskId: TaskId,
        val memberId: MemberId
    ) : TaskCommand

    /**
     * 회고 연결 커맨드
     */
    data class LinkRetrospective(
        val taskId: TaskId,
        val retrospectiveId: RetrospectiveId
    ) : TaskCommand
}
