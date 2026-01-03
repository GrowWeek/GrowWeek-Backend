package xyz.robinjoon.growweek.task.domain.model

@JvmInline
value class TaskTitle(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "제목은 비어있을 수 없습니다" }
        require(value.length <= 50) { "제목은 50자 이하여야 합니다" }
    }
}
