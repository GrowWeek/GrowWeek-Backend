package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.task.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime

class GeminiQuestionGenerationServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val geminiClient = mockk<GeminiClient>()
        val service = GeminiQuestionGenerationService(geminiClient)

        Given("질문 생성 요청이 왔을 때") {

            val tasks =
                listOf(
                    createTask(TaskId(1L), "프로젝트 기획서 작성", TaskStatus.DONE),
                    createTask(TaskId(2L), "코드 리뷰", TaskStatus.IN_PROGRESS),
                    createTask(TaskId(3L), "회의 준비", TaskStatus.TODO),
                )
            val questionCount = QuestionCount(3)

            When("Gemini API가 정상 응답을 반환하면") {
                val apiResponse =
                    """["이번 주 프로젝트 기획서 작성에서 가장 중요하게 고려한 점은 무엇인가요?", "코드 리뷰를 진행하면서 발견한 개선점이 있나요?", "다음 주 계획을 세울 때 이번 주 경험을 어떻게 반영하시겠어요?"]"""

                every { geminiClient.generateContent(any()) } returns apiResponse

                val result = runBlocking { service.generateQuestions(tasks, questionCount) }

                Then("3개의 질문이 생성되어야 한다") {
                    result.size shouldBe 3
                }

                Then("JSON 배열이 파싱되어야 한다") {
                    result[0] shouldBe "이번 주 프로젝트 기획서 작성에서 가장 중요하게 고려한 점은 무엇인가요?"
                }

                Then("GeminiClient가 호출되어야 한다") {
                    verify { geminiClient.generateContent(any()) }
                }
            }

            When("요청 개수보다 많은 질문이 반환되면") {
                val apiResponse = """["질문1", "질문2", "질문3", "질문4", "질문5"]"""

                every { geminiClient.generateContent(any()) } returns apiResponse

                val result = runBlocking { service.generateQuestions(tasks, QuestionCount(3)) }

                Then("요청한 개수만큼만 반환되어야 한다") {
                    result.size shouldBe 3
                }
            }
        }

        Given("Gemini API 호출이 실패했을 때") {

            val tasks =
                listOf(
                    createTask(TaskId(1L), "완료된 작업", TaskStatus.DONE),
                )
            val questionCount = QuestionCount(3)

            When("GeminiApiException이 발생하면") {
                every { geminiClient.generateContent(any()) } throws GeminiApiException("API 호출 실패")

                val result = runBlocking { service.generateQuestions(tasks, questionCount) }

                Then("fallback 질문이 반환되어야 한다") {
                    result.size shouldBe 3
                }

                Then("할일 기반 질문이 포함되어야 한다") {
                    result.any { it.contains("완료된 작업") } shouldBe true
                }
            }

            When("일반 예외가 발생하면") {
                every { geminiClient.generateContent(any()) } throws RuntimeException("알 수 없는 오류")

                val result = runBlocking { service.generateQuestions(tasks, questionCount) }

                Then("fallback 질문이 반환되어야 한다") {
                    result.size shouldBe 3
                }
            }
        }

        Given("할일 목록이 비어있을 때") {

            val emptyTasks = emptyList<Task>()
            val questionCount = QuestionCount(3)

            When("질문 생성을 요청하면") {
                val apiResponse = """["이번 주 목표를 달성하기 어려웠던 이유는 무엇인가요?", "다음 주에 집중하고 싶은 영역은?", "업무 외적으로 배운 점이 있나요?"]"""

                every { geminiClient.generateContent(any()) } returns apiResponse

                val result = runBlocking { service.generateQuestions(emptyTasks, questionCount) }

                Then("질문이 생성되어야 한다") {
                    result.size shouldBe 3
                }
            }
        }

        Given("다양한 상태의 할일이 있을 때") {

            val tasks =
                listOf(
                    createTask(TaskId(1L), "완료된 작업 A", TaskStatus.DONE),
                    createTask(TaskId(2L), "완료된 작업 B", TaskStatus.DONE),
                    createTask(TaskId(3L), "진행 중인 작업", TaskStatus.IN_PROGRESS),
                    createTask(TaskId(4L), "시작 전 작업", TaskStatus.TODO),
                )

            When("프롬프트를 생성하면") {
                var capturedPrompt: String? = null
                every { geminiClient.generateContent(any()) } answers {
                    capturedPrompt = firstArg()
                    """["질문1", "질문2", "질문3"]"""
                }

                runBlocking { service.generateQuestions(tasks, QuestionCount(3)) }

                Then("완료된 할일 정보가 프롬프트에 포함되어야 한다") {
                    capturedPrompt shouldContainSubstring "완료된 할일"
                    capturedPrompt shouldContainSubstring "완료된 작업 A"
                }

                Then("진행 중인 할일 정보가 프롬프트에 포함되어야 한다") {
                    capturedPrompt shouldContainSubstring "진행 중인 할일"
                    capturedPrompt shouldContainSubstring "진행 중인 작업"
                }

                Then("시작하지 않은 할일 정보가 프롬프트에 포함되어야 한다") {
                    capturedPrompt shouldContainSubstring "시작하지 않은 할일"
                    capturedPrompt shouldContainSubstring "시작 전 작업"
                }
            }
        }

        Given("JSON이 아닌 텍스트 응답이 왔을 때") {

            val tasks = listOf(createTask(TaskId(1L), "작업", TaskStatus.DONE))

            When("번호가 붙은 질문 목록이 반환되면") {
                val textResponse =
                    """
                    1. 이번 주 가장 잘한 일은 무엇인가요?
                    2. 어려웠던 점은 무엇인가요?
                    3. 다음 주 계획은?
                    """.trimIndent()

                every { geminiClient.generateContent(any()) } returns textResponse

                val result = runBlocking { service.generateQuestions(tasks, QuestionCount(3)) }

                Then("텍스트 파싱으로 질문이 추출되어야 한다") {
                    result.isNotEmpty() shouldBe true
                }
            }
        }
    })

private fun createTask(
    taskId: TaskId,
    title: String,
    status: TaskStatus,
): Task {
    val now = LocalDateTime.now()
    val weekId = WeekId.of(LocalDate.now().minusDays(7))
    return Task(
        id = taskId,
        memberId = MemberId(1L),
        title = TaskTitle(title),
        description = TaskDescription("설명"),
        status = status,
        sensitivityLevel = SensitivityLevel.NONE,
        priority = Priority(1),
        weekId = weekId,
        dueDate = weekId.endDate,
        createdAt = now,
        updatedAt = now,
        retrospectiveId = null,
    )
}

private infix fun String?.shouldContainSubstring(substring: String) {
    this?.contains(substring) shouldBe true
}
