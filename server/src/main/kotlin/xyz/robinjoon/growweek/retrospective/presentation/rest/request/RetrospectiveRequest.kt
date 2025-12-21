package xyz.robinjoon.growweek.retrospective.presentation.rest.request

data class CreateRetrospectiveRequest(
    val startDate: String,
    val endDate: String,
    val questionCount: Int = 3
)

data class WriteAnswerRequest(
    val questionId: Long,
    val content: String?
)

data class WriteAdditionalNotesRequest(
    val notes: String
)
