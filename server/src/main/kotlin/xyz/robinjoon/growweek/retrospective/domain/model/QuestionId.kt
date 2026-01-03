package xyz.robinjoon.growweek.retrospective.domain.model

@JvmInline
value class QuestionId(
    val value: Long,
) {
    init {
        require(value > 0) { "QuestionId must be greater than 0" }
    }
}
