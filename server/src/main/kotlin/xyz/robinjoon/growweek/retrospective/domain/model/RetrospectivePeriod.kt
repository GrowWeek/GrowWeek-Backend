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
     * 종료일 2일 전(금요일)부터 다음 주 월요일 0시 전까지 작성 가능
     */
    fun isWritable(currentDate: LocalDate = LocalDate.now()): Boolean {
        val writableStartDate = endDate.minusDays(DAYS_BEFORE_END_DATE_TO_START_WRITING)
        val writableEndDate = calculateNextMonday(endDate)
        return !currentDate.isBefore(writableStartDate) && currentDate.isBefore(writableEndDate)
    }

    private fun calculateNextMonday(date: LocalDate): LocalDate {
        val daysUntilMonday = (DayOfWeek.MONDAY.value - date.dayOfWeek.value + 7) % 7
        return if (daysUntilMonday == 0) {
            date.plusDays(7)
        } else {
            date.plusDays(daysUntilMonday.toLong())
        }
    }

    companion object {
        private const val DAYS_BEFORE_END_DATE_TO_START_WRITING = 2L
    }
}
