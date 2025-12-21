package xyz.robinjoon.growweek.retrospective.domain.model

@JvmInline
value class AnswerId(val value: Long) {
    init {
        require(value > 0) { "AnswerId must be greater than 0" }
    }
}
