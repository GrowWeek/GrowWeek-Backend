package xyz.robinjoon.growweek.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime

object TaskTable : LongIdTable("tasks") {
    // 기본 필드
    val userId = long("user_id")
    val title = varchar("title", 50)
    val description = text("description").nullable()

    // 상태 및 속성
    val status = varchar("status", 20)
    val sensitivityLevel = varchar("sensitivity_level", 20).default("NONE")
    val priority = integer("priority")

    // 기간
    val startDate = date("start_date")
    val dueDate = date("due_date")

    // 감사 정보
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    // 회고 연결
    val retrospectiveId = long("retrospective_id").nullable()

    init {
        // 복합 인덱스
        index(false, userId, startDate, dueDate)
        index(false, userId)
        index(false, retrospectiveId)
        index(false, status)
    }
}
