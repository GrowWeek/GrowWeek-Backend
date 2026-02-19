package xyz.robinjoon.growweek.common.contract.task

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.WeekId

data class TaskSummaryPayload(
    val memberId: MemberId,
    val weekId: WeekId,
    val size: Int = 100,
)
