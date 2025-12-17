package xyz.robinjoon.growweek.task.presentation.rest.request

data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val priority: Int,
    val startDate: String, // yyyy-MM-dd
    val dueDate: String,   // yyyy-MM-dd
    val sensitivityLevel: String? = "NONE"
)
