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
    ) : TaskApplicationCommand {
        constructor(
            memberId: Long,
            title: String,
            description: String?,
            priority: Int,
            dueDate: String,
            sensitivityLevel: String? = "NONE",
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            memberId = MemberId(memberId),
            title = title,
            description = description,
            priority = priority,
            dueDate = LocalDate.parse(dueDate),
            sensitivityLevel = sensitivityLevel?.let { SensitivityLevel.valueOf(it) } ?: SensitivityLevel.NONE,
        )
    }

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
    ) : TaskApplicationCommand {
        constructor(
            taskId: Long,
            memberId: Long,
            title: String?,
            description: String?,
            status: String?,
            priority: Int?,
            dueDate: String?,
            sensitivityLevel: String?,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            taskId = TaskId(taskId),
            memberId = MemberId(memberId),
            title = title,
            description = description,
            status = status?.let { TaskStatus.valueOf(it) },
            priority = priority,
            dueDate = dueDate?.let { LocalDate.parse(it) },
            sensitivityLevel = sensitivityLevel?.let { SensitivityLevel.valueOf(it) },
        )
    }

    /**
     * 할일 상태 변경 커맨드
     */
    data class UpdateTaskStatus(
        val taskId: TaskId,
        val memberId: MemberId,
        val status: TaskStatus,
    ) : TaskApplicationCommand {
        constructor(
            taskId: Long,
            memberId: Long,
            status: String,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            taskId = TaskId(taskId),
            memberId = MemberId(memberId),
            status = TaskStatus.valueOf(status),
        )
    }

    /**
     * 할일 삭제 커맨드
     */
    data class DeleteTask(
        val taskId: TaskId,
        val memberId: MemberId,
    ) : TaskApplicationCommand {
        constructor(
            taskId: Long,
            memberId: Long,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            taskId = TaskId(taskId),
            memberId = MemberId(memberId),
        )
    }
}
