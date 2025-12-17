package xyz.robinjoon.growweek.task.domain.model

@JvmInline
value class TaskId(val value: Long) {
    init {
        require(value > 0) { "TaskId must be greater than 0" }
    }
}
