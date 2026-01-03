package xyz.robinjoon.growweek.task.domain.model

import java.time.LocalDate

data class TaskPeriod(
    val startDate: LocalDate,
    val dueDate: LocalDate,
) {
    init {
        require(!dueDate.isBefore(startDate)) {
            "마감일은 시작일보다 이전일 수 없습니다"
        }
    }

    fun overlaps(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Boolean = !(dueDate.isBefore(weekStart) || startDate.isAfter(weekEnd))
}
