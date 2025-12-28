package xyz.robinjoon.growweek.retrospective.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.domain.model.*
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import java.time.LocalDate
import java.time.LocalDateTime

class WriteAdditionalNotesServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val retrospectiveRepository = mockk<RetrospectiveRepository>()
    val service = WriteAdditionalNotesService(retrospectiveRepository)

    Given("기타 회고 내용 작성 요청이 왔을 때") {
        val retrospectiveId = RetrospectiveId(1L)
        val memberId = MemberId(1L)
        val notes = "다음 주에는 더 집중해서 일하자."

        val command = RetrospectiveApplicationCommand.WriteAdditionalNotes(
            retrospectiveId = retrospectiveId,
            memberId = memberId,
            notes = notes
        )

        val now = LocalDateTime.now()
        val updatedRetrospective = Retrospective(
            id = retrospectiveId,
            memberId = memberId,
            period = RetrospectivePeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
            status = RetrospectiveStatus.IN_PROGRESS,
            questionCount = QuestionCount(3),
            questions = emptyList(),
            answers = emptyMap(),
            additionalNotes = AdditionalNotes(notes),
            createdAt = now,
            updatedAt = now
        )

        val commandSlot = slot<List<RetrospectiveCommand>>()
        every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns listOf(updatedRetrospective)

        When("서비스를 실행하면") {
            val result = service.execute(command)

            Then("Repository에 저장 요청을 해야 한다") {
                verify(exactly = 1) { retrospectiveRepository.saveAll(any()) }
            }

            Then("Application Command가 Domain Command로 변환되어야 한다") {
                val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.WriteAdditionalNotes
                capturedCommand.retrospectiveId shouldBe retrospectiveId
                capturedCommand.memberId shouldBe memberId
                capturedCommand.notes shouldBe AdditionalNotes(notes)
            }

            Then("기타 회고 내용이 작성된 회고 DTO를 반환해야 한다") {
                result.id shouldBe retrospectiveId
                result.additionalNotes shouldBe notes
            }
        }
    }
})
