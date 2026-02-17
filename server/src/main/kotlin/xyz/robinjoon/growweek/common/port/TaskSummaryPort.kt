package xyz.robinjoon.growweek.common.port

import xyz.robinjoon.growweek.common.domain.TaskSummary
import xyz.robinjoon.growweek.common.domain.TaskSummaryPayload

interface TaskSummaryPort {
    fun getWeeklyTaskSummaries(payload: TaskSummaryPayload): List<TaskSummary>
}
