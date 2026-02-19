package xyz.robinjoon.growweek.task.infrastructure.external

import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.common.contract.task.TaskSummary
import xyz.robinjoon.growweek.common.contract.task.TaskSummaryPayload
import xyz.robinjoon.growweek.common.contract.task.TaskSummaryPort
import xyz.robinjoon.growweek.common.contract.task.TaskSummaryStatus
import xyz.robinjoon.growweek.task.domain.model.Task
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Component
class TaskSummaryPortAdapter(
    private val taskRepository: TaskRepository,
) : TaskSummaryPort {
    override fun getWeeklyTaskSummaries(payload: TaskSummaryPayload): List<TaskSummary> =
        taskRepository
            .findAll(
                TaskQuery.Offset.byMemberIdAndWeek(
                    memberId = payload.memberId,
                    weekId = payload.weekId,
                    size = payload.size,
                ),
            ).items
            .map { it.toTaskSummary() }

    private fun Task.toTaskSummary(): TaskSummary =
        TaskSummary(
            title = title.value,
            description = description?.value,
            status = status.toTaskSummaryStatus(),
            sensitivityLevel = sensitivityLevel,
        )

    private fun TaskStatus.toTaskSummaryStatus(): TaskSummaryStatus =
        when (this) {
            TaskStatus.TODO -> TaskSummaryStatus.TODO
            TaskStatus.IN_PROGRESS -> TaskSummaryStatus.IN_PROGRESS
            TaskStatus.DONE -> TaskSummaryStatus.DONE
            TaskStatus.CANCEL -> TaskSummaryStatus.CANCEL
        }
}
