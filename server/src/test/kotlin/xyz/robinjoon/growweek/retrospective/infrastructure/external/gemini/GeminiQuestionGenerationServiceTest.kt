package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import xyz.robinjoon.growweek.common.contract.task.TaskSummary
import xyz.robinjoon.growweek.common.contract.task.TaskSummaryStatus
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount

class GeminiQuestionGenerationServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val geminiClient = mockk<GeminiClient>()
        val service = GeminiQuestionGenerationService(geminiClient)

        Given("질문 생성 요청이 왔을 때") {

            val tasks =
                listOf(
                    createTaskSummary("프로젝트 기획서 작성", TaskSummaryStatus.DONE),
                    createTaskSummary("코드 리뷰", TaskSummaryStatus.IN_PROGRESS),
                    createTaskSummary("회의 준비", TaskSummaryStatus.TODO),
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
                    createTaskSummary("완료된 작업", TaskSummaryStatus.DONE),
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

            val emptyTasks = emptyList<TaskSummary>()
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
                    createTaskSummary("완료된 작업 A", TaskSummaryStatus.DONE),
                    createTaskSummary("완료된 작업 B", TaskSummaryStatus.DONE),
                    createTaskSummary("진행 중인 작업", TaskSummaryStatus.IN_PROGRESS),
                    createTaskSummary("시작 전 작업", TaskSummaryStatus.TODO),
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

            val tasks = listOf(createTaskSummary("작업", TaskSummaryStatus.DONE))

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

private fun createTaskSummary(
    title: String,
    status: TaskSummaryStatus,
): TaskSummary =
    TaskSummary(
        title = title,
        description = "설명",
        status = status,
        sensitivityLevel = SensitivityLevel.NONE,
    )

private infix fun String?.shouldContainSubstring(substring: String) {
    this?.contains(substring) shouldBe true
}
