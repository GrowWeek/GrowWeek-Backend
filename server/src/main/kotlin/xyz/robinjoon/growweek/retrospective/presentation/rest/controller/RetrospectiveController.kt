package xyz.robinjoon.growweek.retrospective.presentation.rest.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.query.RetrospectiveApplicationQuery
import xyz.robinjoon.growweek.retrospective.application.usecase.*
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionId
import xyz.robinjoon.growweek.retrospective.presentation.rest.request.CreateRetrospectiveRequest
import xyz.robinjoon.growweek.retrospective.presentation.rest.request.WriteAdditionalNotesRequest
import xyz.robinjoon.growweek.retrospective.presentation.rest.request.WriteAnswerRequest
import xyz.robinjoon.growweek.retrospective.presentation.rest.response.*
import java.time.LocalDate

/**
 * 회고 관리 API 컨트롤러
 */
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

    /**
     * 새로운 회고를 생성한다.
     *
     * @param request 회고 생성 요청 정보
     * @param userId 사용자 식별자
     * @return 생성된 회고 정보
     */
    @PostMapping
    fun createRetrospective(
        @RequestBody request: CreateRetrospectiveRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.CreateRetrospective(
            memberId = MemberId(userId),
            startDate = LocalDate.parse(request.startDate),
            endDate = LocalDate.parse(request.endDate),
            questionCount = request.questionCount
        )

        val dto = createRetrospectiveUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * AI를 이용하여 회고 질문을 생성한다.
     *
     * @param retrospectiveId 회고 식별자
     * @param userId 사용자 식별자
     * @return 질문이 생성된 회고 정보
     */
    @PostMapping("/{retrospectiveId}/generate-questions")
    fun generateQuestions(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.GenerateQuestions(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(userId)
        )

        val dto = runBlocking {
            generateQuestionsUseCase.execute(command)
        }
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    /**
     * 질문에 대한 답변을 작성한다.
     *
     * @param retrospectiveId 회고 식별자
     * @param request 답변 작성 요청 정보
     * @param userId 사용자 식별자
     * @return 답변이 작성된 회고 정보
     */
    @PostMapping("/{retrospectiveId}/answers")
    fun writeAnswer(
        @PathVariable retrospectiveId: Long,
        @RequestBody request: WriteAnswerRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.WriteAnswer(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(userId),
            questionId = QuestionId(request.questionId),
            content = request.content
        )

        val dto = writeAnswerUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    /**
     * 추가 메모를 작성한다.
     *
     * @param retrospectiveId 회고 식별자
     * @param request 추가 메모 작성 요청 정보
     * @param userId 사용자 식별자
     * @return 메모가 작성된 회고 정보
     */
    @PutMapping("/{retrospectiveId}/additional-notes")
    fun writeAdditionalNotes(
        @PathVariable retrospectiveId: Long,
        @RequestBody request: WriteAdditionalNotesRequest,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.WriteAdditionalNotes(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(userId),
            notes = request.notes
        )

        val dto = writeAdditionalNotesUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    /**
     * 회고를 완료 처리한다.
     *
     * @param retrospectiveId 회고 식별자
     * @param userId 사용자 식별자
     * @return 완료된 회고 정보
     */
    @PostMapping("/{retrospectiveId}/complete")
    fun completeRetrospective(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val command = RetrospectiveApplicationCommand.CompleteRetrospective(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(userId)
        )

        val dto = completeRetrospectiveUseCase.execute(command)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    /**
     * 회고를 삭제한다.
     *
     * @param retrospectiveId 회고 식별자
     * @param userId 사용자 식별자
     * @return 204 No Content
     */
    @DeleteMapping("/{retrospectiveId}")
    fun deleteRetrospective(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<Void> {
        val command = RetrospectiveApplicationCommand.DeleteRetrospective(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(userId)
        )

        deleteRetrospectiveUseCase.execute(command)

        return ResponseEntity.noContent().build()
    }

    /**
     * 회고 상세 정보를 조회한다.
     *
     * @param retrospectiveId 회고 식별자
     * @param userId 사용자 식별자
     * @return 회고 상세 정보
     */
    @GetMapping("/{retrospectiveId}")
    fun getRetrospective(
        @PathVariable retrospectiveId: Long,
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<RetrospectiveResponse> {
        val query = RetrospectiveApplicationQuery.ByRetrospectiveId(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(userId)
        )

        val dto = getRetrospectiveUseCase.getById(query)
        val response = RetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }

    /**
     * 사용자의 회고 목록을 조회한다.
     *
     * @param userId 사용자 식별자
     * @param page 페이지 번호 (오프셋 기반, 0부터 시작)
     * @param size 페이지 크기 (기본값: 20)
     * @param cursor 커서 (커서 기반 페이징 시 사용)
     * @return 회고 목록 (페이지네이션 적용)
     */
    @GetMapping
    fun getRetrospectives(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<PageResponse<RetrospectiveSummaryResponse>> {
        val pageDto = if (cursor != null) {
            val query = RetrospectiveApplicationQuery.Cursor.byMemberId(
                memberId = MemberId(userId),
                cursor = cursor,
                size = size ?: 20
            )
            getRetrospectiveUseCase.getList(query)
        } else {
            val query = RetrospectiveApplicationQuery.Offset.byMemberId(
                memberId = MemberId(userId),
                page = page ?: 0,
                size = size ?: 20
            )
            getRetrospectiveUseCase.getList(query)
        }

        val response = pageDto.toResponse { RetrospectiveSummaryResponse.from(it) }

        return ResponseEntity.ok(response)
    }

    /**
     * 월간 회고 목록과 통계를 조회한다.
     *
     * @param userId 사용자 식별자
     * @param year 조회할 년도
     * @param month 조회할 월 (1-12)
     * @return 월간 회고 목록과 통계
     */
    @GetMapping("/monthly")
    fun getMonthlyRetrospectives(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<MonthlyRetrospectiveResponse> {
        val query = RetrospectiveApplicationQuery.Offset.byMemberIdAndMonth(
            memberId = MemberId(userId),
            year = year,
            month = month,
            size = 31
        )

        val dto = getMonthlyRetrospectivesUseCase.execute(query)
        val response = MonthlyRetrospectiveResponse.from(dto)

        return ResponseEntity.ok(response)
    }
}
