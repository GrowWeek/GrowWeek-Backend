package xyz.robinjoon.growweek.common.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

@JvmInline
value class WeekId(
    val value: String,
) {
    init {
        require(value.matches(WEEK_ID_PATTERN)) {
            "WeekId must be in format 'YYYY-Www' (e.g., '2024-W03'), but was '$value'"
        }
    }

    val year: Int get() = value.substringBefore("-W").toInt()

    val weekNumber: Int get() = value.substringAfter("-W").toInt()

    val startDate: LocalDate
        get() =
            LocalDate
                .of(year, 1, 4)
                .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNumber.toLong())
                .with(DayOfWeek.MONDAY)

    val endDate: LocalDate get() = startDate.plusDays(6)

    fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    companion object {
        private val WEEK_ID_PATTERN = Regex("""\d{4}-W(0[1-9]|[1-4]\d|5[0-3])""")

        fun of(
            year: Int,
            weekNumber: Int,
        ): WeekId {
            require(weekNumber in 1..53) { "weekNumber must be between 1 and 53, but was $weekNumber" }
            return WeekId("$year-W${weekNumber.toString().padStart(2, '0')}")
        }

        fun of(date: LocalDate): WeekId {
            val weekFields = WeekFields.ISO
            val year = date.get(weekFields.weekBasedYear())
            val week = date.get(weekFields.weekOfWeekBasedYear())
            return of(year, week)
        }
    }
}
