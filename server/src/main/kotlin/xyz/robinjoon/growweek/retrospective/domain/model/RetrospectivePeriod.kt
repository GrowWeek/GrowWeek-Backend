package xyz.robinjoon.growweek.retrospective.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class RetrospectivePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    init {
        require(!startDate.isAfter(endDate)) {
            "시작일은 종료일보다 이전이어야 합니다"
        }
    }

    /**
     * 회고 작성 가능 여부 확인
     * 다음 주 월요일 0시까지 작성 가능
     */
    fun isWritable(currentDate: LocalDate = LocalDate.now()): Boolean {
        val nextMonday = calculateNextMonday(endDate)
        return !currentDate.isAfter(nextMonday)
    }

    private fun calculateNextMonday(date: LocalDate): LocalDate {
        val daysUntilMonday = (DayOfWeek.MONDAY.value - date.dayOfWeek.value + 7) % 7
        return if (daysUntilMonday == 0) {
            date.plusDays(7)
        } else {
            date.plusDays(daysUntilMonday.toLong())
        }
    }
}
