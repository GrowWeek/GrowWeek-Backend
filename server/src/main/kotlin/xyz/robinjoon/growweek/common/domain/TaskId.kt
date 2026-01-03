package xyz.robinjoon.growweek.common.domain

@JvmInline
value class TaskId(
    val value: Long,
) {
    init {
        require(value > 0) { "TaskId must be greater than 0" }
    }
}
