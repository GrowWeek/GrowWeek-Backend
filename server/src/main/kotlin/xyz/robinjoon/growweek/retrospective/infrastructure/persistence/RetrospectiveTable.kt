package xyz.robinjoon.growweek.retrospective.infrastructure.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime

object RetrospectiveTable : LongIdTable("retrospectives") {
    val userId = long("user_id")
    val weekId = varchar("week_id", 10)
    val status = varchar("status", 50)
    val questionCount = integer("question_count")
    val additionalNotes = text("additional_notes").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index(false, userId)
        index(false, weekId)
        index(false, status)
    }
}

object QuestionTable : LongIdTable("questions") {
    val retrospectiveId = long("retrospective_id").references(RetrospectiveTable.id)
    val content = text("content")
    val order = integer("question_order")
    val createdAt = datetime("created_at")

    init {
        index(false, retrospectiveId)
    }
}

object AnswerTable : LongIdTable("answers") {
    val questionId = long("question_id").references(QuestionTable.id)
    val content = text("content").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index(false, questionId)
    }
}
