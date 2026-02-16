package xyz.robinjoon.growweek.common.domain

data class TaskSummary(
    val title: String,
    val description: String?,
    val status: TaskSummaryStatus,
    val sensitivityLevel: SensitivityLevel,
)
