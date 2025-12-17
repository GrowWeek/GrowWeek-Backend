package xyz.robinjoon.growweek.common.domain

@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be greater than 0" }
    }
}
