package xyz.robinjoon.growweek.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * 완료된 회고 기간 테이블
 *
 * 회고가 완료되면 해당 기간 정보를 저장합니다.
 * Task 생성/수정 시 이 테이블을 조회하여 회고 완료된 기간에 대한 검증에 사용합니다.
 */
object CompletedRetrospectivePeriodTable : Table("completed_retrospective_periods") {
    val retrospectiveId = long("retrospective_id")
    val memberId = long("member_id")
    val startDate = date("start_date")
    val endDate = date("end_date")
    val completedAt = datetime("completed_at")

    override val primaryKey = PrimaryKey(retrospectiveId)

    init {
        // 회원별 기간 조회를 위한 인덱스
        index(false, memberId, startDate, endDate)
        index(false, memberId)
    }
}
