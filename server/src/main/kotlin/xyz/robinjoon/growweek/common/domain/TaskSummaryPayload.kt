package xyz.robinjoon.growweek.common.domain

data class TaskSummaryPayload(
    val memberId: MemberId,
    val weekId: WeekId,
    val size: Int = 100,
)
