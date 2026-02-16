package xyz.robinjoon.growweek.retrospective.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskSummary
import xyz.robinjoon.growweek.common.domain.TaskSummaryPayload
import xyz.robinjoon.growweek.common.domain.TaskSummaryStatus
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.common.port.TaskSummaryPort
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.domain.model.Question
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionId
import xyz.robinjoon.growweek.retrospective.domain.model.Retrospective
import xyz.robinjoon.growweek.retrospective.domain.model.RetrospectiveStatus
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.model.query.RetrospectiveQuery
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import xyz.robinjoon.growweek.retrospective.domain.service.QuestionGenerationService
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * MockK에서 value class (QuestionCount) 문제를 피하기 위한 Fake 구현체
 */
class FakeQuestionGenerationService : QuestionGenerationService {
    var returnValue: List<String> = emptyList()
    var capturedTasks: List<TaskSummary>? = null
    var capturedQuestionCount: QuestionCount? = null

    override suspend fun generateQuestions(
        tasks: List<TaskSummary>,
        questionCount: QuestionCount,
    ): List<String> {
        capturedTasks = tasks
        capturedQuestionCount = questionCount
        return returnValue
    }
}

class GenerateQuestionsServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val retrospectiveRepository = mockk<RetrospectiveRepository>()
        val taskSummaryPort = mockk<TaskSummaryPort>()
        val fakeQuestionGenerationService = FakeQuestionGenerationService()
        val service = GenerateQuestionsService(retrospectiveRepository, taskSummaryPort, fakeQuestionGenerationService)

        Given("질문 생성 요청이 왔을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)
            val startDate = LocalDate.of(2025, 1, 6)
            LocalDate.of(2025, 1, 12)
            val weekId = WeekId.of(startDate)
            val now = LocalDateTime.now()

            val command =
                RetrospectiveApplicationCommand.GenerateQuestions(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                )

            val existingRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    weekId = weekId,
                    status = RetrospectiveStatus.TODO,
                    questionCount = QuestionCount(3),
                    questions = emptyList(),
                    answers = emptyMap(),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val taskSummaries =
                listOf(
                    createTaskSummary("할일 1", "설명 1", TaskSummaryStatus.TODO, SensitivityLevel.NONE),
                    createTaskSummary("할일 2", "설명 2", TaskSummaryStatus.TODO, SensitivityLevel.TITLE_ONLY),
                    createTaskSummary("할일 3", "설명 3", TaskSummaryStatus.TODO, SensitivityLevel.NEVER),
                )

            val generatedQuestionContents =
                listOf(
                    "이번 주 가장 생산적이었던 순간은 언제인가요?",
                    "다음 주에 개선하고 싶은 점이 있나요?",
                    "이번 주 배운 것 중 가장 인상 깊었던 것은?",
                )

            val questions =
                generatedQuestionContents.mapIndexed { index, content ->
                    Question(
                        id = QuestionId((index + 1).toLong()),
                        retrospectiveId = retrospectiveId,
                        content = content,
                        order = index,
                        createdAt = now,
                    )
                }

            val completedRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    weekId = weekId,
                    status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                    questionCount = QuestionCount(3),
                    questions = questions,
                    answers = emptyMap(),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            every { retrospectiveRepository.findAll(any<RetrospectiveQuery.OffsetByRetrospectiveId>()) } returns
                OffsetPage(size = 1, page = 0, totalPage = 1, items = listOf(existingRetrospective))

            every {
                retrospectiveRepository.saveAll(
                    match { commands ->
                        commands.any { it is RetrospectiveCommand.GenerateQuestions }
                    },
                )
            } returns listOf(existingRetrospective.copy(status = RetrospectiveStatus.BEFORE_GENERATE_QUESTION))

            every { taskSummaryPort.getWeeklyTaskSummaries(any()) } returns taskSummaries

            fakeQuestionGenerationService.returnValue = generatedQuestionContents

            every {
                retrospectiveRepository.saveAll(
                    match { commands ->
                        commands.any { it is RetrospectiveCommand.CompleteQuestionGeneration }
                    },
                )
            } returns listOf(completedRetrospective)

            When("서비스를 실행하면") {
                val result = runBlocking { service.execute(command) }

                Then("회고를 조회해야 한다") {
                    verify { retrospectiveRepository.findAll(any<RetrospectiveQuery.OffsetByRetrospectiveId>()) }
                }

                Then("질문 생성 시작 상태로 변경해야 한다") {
                    verify {
                        retrospectiveRepository.saveAll(
                            match { commands ->
                                commands.any { it is RetrospectiveCommand.GenerateQuestions }
                            },
                        )
                    }
                }

                Then("해당 기간의 할일 요약을 조회해야 한다") {
                    verify {
                        taskSummaryPort.getWeeklyTaskSummaries(
                            TaskSummaryPayload(
                                memberId = memberId,
                                weekId = weekId,
                                size = 100,
                            ),
                        )
                    }
                }

                Then("AI 질문 생성 서비스를 호출해야 한다") {
                    fakeQuestionGenerationService.capturedQuestionCount shouldBe QuestionCount(3)
                }

                Then("질문 생성 완료 상태로 변경해야 한다") {
                    verify {
                        retrospectiveRepository.saveAll(
                            match { commands ->
                                commands.any { it is RetrospectiveCommand.CompleteQuestionGeneration }
                            },
                        )
                    }
                }

                Then("질문이 생성된 회고 DTO를 반환해야 한다") {
                    result.id shouldBe retrospectiveId
                    result.status shouldBe RetrospectiveStatus.AFTER_GENERATE_QUESTION
                    result.questions.size shouldBe 3
                }
            }
        }

        Given("민감도가 NEVER인 할일이 포함되어 있을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)
            val startDate = LocalDate.of(2025, 1, 6)
            LocalDate.of(2025, 1, 12)
            val weekId = WeekId.of(startDate)
            val now = LocalDateTime.now()

            val command =
                RetrospectiveApplicationCommand.GenerateQuestions(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                )

            val existingRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    weekId = weekId,
                    status = RetrospectiveStatus.TODO,
                    questionCount = QuestionCount(3),
                    questions = emptyList(),
                    answers = emptyMap(),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val taskSummaries =
                listOf(
                    createTaskSummary("할일 1", "설명 1", TaskSummaryStatus.TODO, SensitivityLevel.NONE),
                    createTaskSummary("비밀 할일", "비밀 설명", TaskSummaryStatus.TODO, SensitivityLevel.NEVER),
                )

            val generatedQuestionContents = listOf("질문1", "질문2", "질문3")

            val questions =
                generatedQuestionContents.mapIndexed { index, content ->
                    Question(
                        id = QuestionId((index + 1).toLong()),
                        retrospectiveId = retrospectiveId,
                        content = content,
                        order = index,
                        createdAt = now,
                    )
                }

            val completedRetrospective =
                existingRetrospective.copy(
                    status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                    questions = questions,
                )

            every { retrospectiveRepository.findAll(any<RetrospectiveQuery.OffsetByRetrospectiveId>()) } returns
                OffsetPage(size = 1, page = 0, totalPage = 1, items = listOf(existingRetrospective))

            every { retrospectiveRepository.saveAll(any()) } returns listOf(completedRetrospective)

            every { taskSummaryPort.getWeeklyTaskSummaries(any()) } returns taskSummaries

            fakeQuestionGenerationService.returnValue = generatedQuestionContents

            When("서비스를 실행하면") {
                runBlocking { service.execute(command) }

                Then("NEVER 민감도의 할일은 필터링되어야 한다") {
                    val filteredTasks = fakeQuestionGenerationService.capturedTasks!!
                    filteredTasks.size shouldBe 1
                    filteredTasks.none { it.sensitivityLevel == SensitivityLevel.NEVER } shouldBe true
                }
            }
        }

        Given("민감도가 TITLE_ONLY인 할일이 포함되어 있을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)
            val startDate = LocalDate.of(2025, 1, 6)
            LocalDate.of(2025, 1, 12)
            val weekId = WeekId.of(startDate)
            val now = LocalDateTime.now()

            val command =
                RetrospectiveApplicationCommand.GenerateQuestions(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                )

            val existingRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    weekId = weekId,
                    status = RetrospectiveStatus.TODO,
                    questionCount = QuestionCount(3),
                    questions = emptyList(),
                    answers = emptyMap(),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val taskSummaries =
                listOf(
                    createTaskSummary("제목만 공개", "비밀 설명", TaskSummaryStatus.TODO, SensitivityLevel.TITLE_ONLY),
                )

            val generatedQuestionContents = listOf("질문1", "질문2", "질문3")

            val questions =
                generatedQuestionContents.mapIndexed { index, content ->
                    Question(
                        id = QuestionId((index + 1).toLong()),
                        retrospectiveId = retrospectiveId,
                        content = content,
                        order = index,
                        createdAt = now,
                    )
                }

            val completedRetrospective =
                existingRetrospective.copy(
                    status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                    questions = questions,
                )

            every { retrospectiveRepository.findAll(any<RetrospectiveQuery.OffsetByRetrospectiveId>()) } returns
                OffsetPage(size = 1, page = 0, totalPage = 1, items = listOf(existingRetrospective))

            every { retrospectiveRepository.saveAll(any()) } returns listOf(completedRetrospective)

            every { taskSummaryPort.getWeeklyTaskSummaries(any()) } returns taskSummaries

            fakeQuestionGenerationService.returnValue = generatedQuestionContents

            When("서비스를 실행하면") {
                runBlocking { service.execute(command) }

                Then("TITLE_ONLY 민감도의 할일은 설명이 null로 변경되어야 한다") {
                    val filteredTasks = fakeQuestionGenerationService.capturedTasks!!
                    filteredTasks.size shouldBe 1
                    filteredTasks.first().description shouldBe null
                }
            }
        }
    })

private fun createTaskSummary(
    title: String,
    description: String,
    status: TaskSummaryStatus,
    sensitivityLevel: SensitivityLevel,
): TaskSummary =
    TaskSummary(
        title = title,
        description = description,
        status = status,
        sensitivityLevel = sensitivityLevel,
    )

private fun <T> runBlocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
