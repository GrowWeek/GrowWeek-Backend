package xyz.robinjoon.growweek.task.presentation.rest.request

data class UpdateTaskRequest(
    val title: String?,
    val description: String?,
    val status: String?,
    val priority: Int?,
    val dueDate: String?, // yyyy-MM-dd
    val sensitivityLevel: String?
)
