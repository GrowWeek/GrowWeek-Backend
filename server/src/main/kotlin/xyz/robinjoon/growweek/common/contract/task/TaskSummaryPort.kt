package xyz.robinjoon.growweek.common.contract.task

interface TaskSummaryPort {
    fun getWeeklyTaskSummaries(payload: TaskSummaryPayload): List<TaskSummary>
}
