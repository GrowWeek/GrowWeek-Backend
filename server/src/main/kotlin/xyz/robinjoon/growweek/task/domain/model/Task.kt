package xyz.robinjoon.growweek.task.domain.model

import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime

data class Task(
    val id: TaskId,
    val userId: UserId,
    val title: TaskTitle,
    val description: TaskDescription?,
    val status: TaskStatus,
    val sensitivityLevel: SensitivityLevel,
    val priority: Priority,
    val period: TaskPeriod,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val retrospectiveId: RetrospectiveId? = null
) {
    /**
     * 할일 수정 가능 여부 확인
     */
    fun canModify(): Boolean {
        // 회고가 없으면 수정 가능
        if (retrospectiveId == null) return true
        // 마감일이 현재 시점 이후면 제한적 수정 가능
        return period.dueDate.isAfter(LocalDate.now())
    }

    /**
     * 제목 수정
     */
    fun updateTitle(newTitle: TaskTitle, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(title = newTitle, updatedAt = LocalDateTime.now())
    }

    /**
     * 설명 수정
     */
    fun updateDescription(newDescription: TaskDescription?, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(description = newDescription, updatedAt = LocalDateTime.now())
    }

    /**
     * 상태 수정
     */
    fun updateStatus(newStatus: TaskStatus, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(status = newStatus, updatedAt = LocalDateTime.now())
    }

    /**
     * 중요도 수정
     */
    fun updatePriority(newPriority: Priority, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(priority = newPriority, updatedAt = LocalDateTime.now())
    }

    /**
     * 민감도 수정
     */
    fun updateSensitivityLevel(newLevel: SensitivityLevel, retrospectiveDate: LocalDate?): Task {
        validateModification(retrospectiveDate)
        return copy(sensitivityLevel = newLevel, updatedAt = LocalDateTime.now())
    }

    /**
     * 마감일 수정
     */
    fun updateDueDate(newDueDate: LocalDate, retrospectiveDate: LocalDate?): Task {
        if (retrospectiveId != null && retrospectiveDate != null) {
            require(newDueDate.isAfter(retrospectiveDate)) {
                "회고 시점 이전으로 마감일을 수정할 수 없습니다"
            }
        }
        val newPeriod = period.copy(dueDate = newDueDate)
        return copy(period = newPeriod, updatedAt = LocalDateTime.now())
    }

    /**
     * 회고 연결
     */
    fun linkRetrospective(retrospectiveId: RetrospectiveId): Task {
        return copy(retrospectiveId = retrospectiveId, updatedAt = LocalDateTime.now())
    }

    /**
     * 특정 주에 속하는지 확인
     */
    fun belongsToWeek(weekStart: LocalDate, weekEnd: LocalDate): Boolean {
        // 시작일~마감일 범위가 해당 주와 겹치는지 확인
        if (!period.overlaps(weekStart, weekEnd)) return false

        // 마감일 이전에 완료된 경우 제외
        if (status == TaskStatus.DONE && updatedAt.toLocalDate().isBefore(period.dueDate)) {
            return false
        }

        return true
    }

    private fun validateModification(retrospectiveDate: LocalDate?) {
        if (retrospectiveId != null && retrospectiveDate != null) {
            require(period.dueDate.isAfter(retrospectiveDate)) {
                "회고가 작성된 할일은 수정할 수 없습니다"
            }
        }
    }
}
