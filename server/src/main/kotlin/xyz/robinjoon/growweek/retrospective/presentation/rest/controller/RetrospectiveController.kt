package xyz.robinjoon.growweek.retrospective.presentation.rest.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.query.RetrospectiveApplicationQuery
import xyz.robinjoon.growweek.retrospective.application.usecase.*
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionId
import xyz.robinjoon.growweek.retrospective.presentation.rest.request.CreateRetrospectiveRequest
import xyz.robinjoon.growweek.retrospective.presentation.rest.request.WriteAdditionalNotesRequest
import xyz.robinjoon.growweek.retrospective.presentation.rest.request.WriteAnswerRequest
import xyz.robinjoon.growweek.retrospective.presentation.rest.response.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/retrospectives")
class RetrospectiveController(
    private val createRetrospectiveUseCase: CreateRetrospectiveUseCase,
    private val generateQuestionsUseCase: GenerateQuestionsUseCase,
    private val writeAnswerUseCase: WriteAnswerUseCase,
    private val writeAdditionalNotesUseCase: WriteAdditionalNotesUseCase,
    private val completeRetrospectiveUseCase: CompleteRetrospectiveUseCase,
    private val deleteRetrospectiveUseCase: DeleteRetrospectiveUseCase,
    private val getRetrospectiveUseCase: GetRetrospectiveUseCase,
    private val getMonthlyRetrospectivesUseCase: GetMonthlyRetrospectivesUseCase
) {

    @PostMapping
    fun createRetrospective(
        @RequestBody request: CreateRetrospectiveRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.CreateRetrospective(
            userId = UserId(userId),
            startDate = LocalDate.parse(request.startDate),
            endDate = LocalDate.parse(request.endDate),
            questionCount = request.questionCount
        )

        val dto = createRetrospectiveUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{retrospectiveId}/generate-questions")
    fun generateQuestions(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.GenerateQuestions(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            userId = UserId(userId)
        )

        val dto = runBlocking {
            generateQuestionsUseCase.execute(command)
        }
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{retrospectiveId}/answers")
    fun writeAnswer(
        @PathVariable retrospectiveId: Long,
        @RequestBody request: WriteAnswerRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.WriteAnswer(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            userId = UserId(userId),
            questionId = QuestionId(request.questionId),
            content = request.content
        )

        val dto = writeAnswerUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    @PutMapping("/{retrospectiveId}/additional-notes")
    fun writeAdditionalNotes(
        @PathVariable retrospectiveId: Long,
        @RequestBody request: WriteAdditionalNotesRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.WriteAdditionalNotes(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            userId = UserId(userId),
            notes = request.notes
        )

        val dto = writeAdditionalNotesUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{retrospectiveId}/complete")
    fun completeRetrospective(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.CompleteRetrospective(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            userId = UserId(userId)
        )

        val dto = completeRetrospectiveUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{retrospectiveId}")
    fun deleteRetrospective(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<Void> {
        val command = RetrospectiveApplicationCommand.DeleteRetrospective(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            userId = UserId(userId)
        )

        deleteRetrospectiveUseCase.execute(command)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{retrospectiveId}")
    fun getRetrospective(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val query = RetrospectiveApplicationQuery.ByRetrospectiveId(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            userId = UserId(userId)
        )

        val dto = getRetrospectiveUseCase.getById(query)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getRetrospectives(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<PageResponse<RetrospectiveSummaryResponse>> {
        val pageDto = if (cursor != null) {
            val query = RetrospectiveApplicationQuery.Cursor.byUserId(
                userId = UserId(userId),
                cursor = cursor,
                size = size ?: 20
            )
            getRetrospectiveUseCase.getList(query)
        } else {
            val query = RetrospectiveApplicationQuery.Offset.byUserId(
                userId = UserId(userId),
                page = page ?: 0,
                size = size ?: 20
            )
            getRetrospectiveUseCase.getList(query)
        }

        val response = pageDto.toResponse { RetrospectiveSummaryResponse.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/monthly")
    fun getMonthlyRetrospectives(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<MonthlyRetrospectiveResponse> {
        val query = RetrospectiveApplicationQuery.Offset.byUserIdAndMonth(
            userId = UserId(userId),
            year = year,
            month = month,
            size = 31
        )

        val dto = getMonthlyRetrospectivesUseCase.execute(query)
        val response = MonthlyRetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }
}
