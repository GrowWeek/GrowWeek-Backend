package xyz.robinjoon.growweek.common.contract.task

import xyz.robinjoon.growweek.common.domain.SensitivityLevel

data class TaskSummary(
    val title: String,
    val description: String?,
    val status: TaskSummaryStatus,
    val sensitivityLevel: SensitivityLevel,
)
