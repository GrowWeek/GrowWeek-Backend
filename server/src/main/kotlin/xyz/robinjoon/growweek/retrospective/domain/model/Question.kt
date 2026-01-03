package xyz.robinjoon.growweek.retrospective.domain.model

import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import java.time.LocalDateTime

data class Question(
    val id: QuestionId,
    val retrospectiveId: RetrospectiveId,
    val content: String,
    val order: Int,
    val createdAt: LocalDateTime,
) {
    init {
        require(content.isNotBlank()) { "질문 내용은 비어있을 수 없습니다" }
        require(order >= 0) { "질문 순서는 0 이상이어야 합니다" }
    }
}
