package xyz.robinjoon.growweek.common.domain

@JvmInline
value class RetrospectiveId(val value: Long) {
    init {
        require(value > 0) { "RetrospectiveId must be greater than 0" }
    }
}
