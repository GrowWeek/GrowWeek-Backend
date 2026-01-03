package xyz.robinjoon.growweek.retrospective.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.retrospective.domain.model.Retrospective
import xyz.robinjoon.growweek.retrospective.domain.model.RetrospectivePeriod
import xyz.robinjoon.growweek.retrospective.domain.model.RetrospectiveStatus
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CreateRetrospectiveServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val retrospectiveRepository = mockk<RetrospectiveRepository>()
        val service = CreateRetrospectiveService(retrospectiveRepository)

        Given("회고 생성 요청이 왔을 때") {
            val memberId = MemberId(1L)
            val startDate = LocalDate.of(2025, 1, 6)
            val endDate = LocalDate.of(2025, 1, 12)
            val questionCount = 3

            val command =
                RetrospectiveApplicationCommand.CreateRetrospective(
                    memberId = memberId,
                    startDate = startDate,
                    endDate = endDate,
                    questionCount = questionCount,
                )

            val savedRetrospective =
                createRetrospective(
                    memberId = memberId,
                    startDate = startDate,
                    endDate = endDate,
                    questionCount = questionCount,
                )

            val commandSlot = slot<List<RetrospectiveCommand>>()
            every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns listOf(savedRetrospective)

            When("서비스를 실행하면") {
                val result = service.execute(command)

                Then("Repository에 저장 요청을 해야 한다") {
                    verify(exactly = 1) { retrospectiveRepository.saveAll(any()) }
                }

                Then("Application Command가 Domain Command로 변환되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.CreateRetrospective
                    capturedCommand.memberId shouldBe memberId
                    capturedCommand.period shouldBe RetrospectivePeriod(startDate, endDate)
                    capturedCommand.questionCount shouldBe QuestionCount(questionCount)
                }

                Then("생성된 회고의 DTO를 반환해야 한다") {
                    result.memberId shouldBe memberId
                    result.startDate shouldBe startDate
                    result.endDate shouldBe endDate
                    result.questionCount shouldBe questionCount
                    result.status shouldBe RetrospectiveStatus.TODO
                }
            }
        }

        Given("질문 개수를 지정하지 않고 회고 생성 요청이 왔을 때") {
            val memberId = MemberId(1L)
            val startDate = LocalDate.of(2025, 1, 6)
            val endDate = LocalDate.of(2025, 1, 12)

            val command =
                RetrospectiveApplicationCommand.CreateRetrospective(
                    memberId = memberId,
                    startDate = startDate,
                    endDate = endDate,
                )

            val savedRetrospective =
                createRetrospective(
                    memberId = memberId,
                    startDate = startDate,
                    endDate = endDate,
                    questionCount = 3,
                )

            val commandSlot = slot<List<RetrospectiveCommand>>()
            every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns listOf(savedRetrospective)

            When("서비스를 실행하면") {
                val result = service.execute(command)

                Then("기본 질문 개수(3)로 설정되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.CreateRetrospective
                    capturedCommand.questionCount shouldBe QuestionCount(3)
                }

                Then("반환된 DTO의 질문 개수가 3이어야 한다") {
                    result.questionCount shouldBe 3
                }
            }
        }
    })

private fun createRetrospective(
    memberId: MemberId,
    startDate: LocalDate,
    endDate: LocalDate,
    questionCount: Int,
): Retrospective {
    val now = LocalDateTime.now()
    return Retrospective(
        id = RetrospectiveId(1L),
        memberId = memberId,
        period = RetrospectivePeriod(startDate, endDate),
        status = RetrospectiveStatus.TODO,
        questionCount = QuestionCount(questionCount),
        questions = emptyList(),
        answers = emptyMap(),
        additionalNotes = null,
        createdAt = now,
        updatedAt = now,
    )
}
