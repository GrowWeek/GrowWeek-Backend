package xyz.robinjoon.growweek.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * 완료된 회고 주 테이블
 *
 * 회고가 완료되면 해당 주 정보를 저장합니다.
 * Task 생성/수정 시 이 테이블을 조회하여 회고 완료된 주에 대한 검증에 사용합니다.
 */
object CompletedWeekTable : Table("completed_weeks") {
    val retrospectiveId = long("retrospective_id")
    val memberId = long("member_id")
    val weekId = varchar("week_id", 10)
    val completedAt = datetime("completed_at")

    override val primaryKey = PrimaryKey(retrospectiveId)

    init {
        // 회원별 주 조회를 위한 인덱스
        index(false, memberId, weekId)
        index(false, memberId)
    }
}
