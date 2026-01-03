package xyz.robinjoon.growweek.task.domain.model

@JvmInline
value class Priority(
    val value: Int,
) {
    init {
        require(value >= 1) { "중요도는 1 이상이어야 합니다" }
    }
}
