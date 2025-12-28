package xyz.robinjoon.growweek.common.domain

@JvmInline
value class MemberId(val value: Long) {
    init {
        require(value >= 0) { "MemberId must be greater than or equal to 0" }
    }
}
