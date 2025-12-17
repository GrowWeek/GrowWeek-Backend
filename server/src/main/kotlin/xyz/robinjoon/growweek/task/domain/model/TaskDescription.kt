package xyz.robinjoon.growweek.task.domain.model

@JvmInline
value class TaskDescription(val value: String) {
    init {
        require(value.length <= 3000) { "설명은 3000자 이하여야 합니다" }
    }
}
