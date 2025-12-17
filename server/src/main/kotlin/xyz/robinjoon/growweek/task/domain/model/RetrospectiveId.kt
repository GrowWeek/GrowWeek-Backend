package xyz.robinjoon.growweek.task.domain.model

@JvmInline
value class RetrospectiveId(val value: Long) {
    init {
        require(value > 0) { "RetrospectiveId must be greater than 0" }
    }
}
