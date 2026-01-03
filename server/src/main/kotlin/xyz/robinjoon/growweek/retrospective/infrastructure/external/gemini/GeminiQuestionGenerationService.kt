package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.retrospective.domain.service.QuestionGenerationService
import xyz.robinjoon.growweek.task.domain.model.Task

/**
 * Gemini API를 활용한 회고 질문 생성 서비스
 *
 * Google Gemini 모델을 사용하여 주간 할일 데이터를 기반으로
 * 개인화된 회고 질문을 생성합니다.
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "gemini", name = ["api-key"])
class GeminiQuestionGenerationService(
    private val geminiClient: GeminiClient,
) : QuestionGenerationService {
    private val logger = LoggerFactory.getLogger(GeminiQuestionGenerationService::class.java)
    private val objectMapper = jacksonObjectMapper()

    override suspend fun generateQuestions(
        tasks: List<Task>,
        questionCount: QuestionCount,
    ): List<String> {
        val prompt = buildPrompt(tasks, questionCount)

        return try {
            val response = geminiClient.generateContent(prompt)
            parseQuestionsFromResponse(response, questionCount.value)
        } catch (e: GeminiApiException) {
            logger.warn("Gemini API 호출 실패, 기본 질문 반환: ${e.message}")
            generateFallbackQuestions(tasks, questionCount)
        } catch (e: Exception) {
            logger.error("질문 생성 중 예외 발생", e)
            generateFallbackQuestions(tasks, questionCount)
        }
    }

    private fun buildPrompt(
        tasks: List<Task>,
        questionCount: QuestionCount,
    ): String {
        val taskSummary =
            if (tasks.isEmpty()) {
                "이번 주에 등록된 할일이 없습니다."
            } else {
                buildTaskSummary(tasks)
            }

        return """
            당신은 개인 성장과 업무 효율성 향상을 돕는 회고 코치입니다.

            다음은 사용자의 이번 주 할일 목록입니다:
            $taskSummary

            위 정보를 바탕으로 사용자가 한 주를 돌아보고 개선점을 찾을 수 있도록 도와주는 회고 질문 ${questionCount.value}개를 생성해주세요.

            질문 작성 가이드라인:
            1. 구체적인 할일 내용을 참조하여 개인화된 질문을 만드세요
            2. 완료된 일에 대해서는 성취감과 학습 포인트를 묻는 질문을 포함하세요
            3. 미완료 또는 진행 중인 일에 대해서는 장애물과 해결 방안을 묻는 질문을 포함하세요
            4. 다음 주 계획과 개선에 대한 질문을 포함하세요
            5. 질문은 한국어로 작성하세요
            6. 각 질문은 명확하고 구체적이어야 합니다

            JSON 배열 형식으로만 응답하세요.
            """.trimIndent()
    }

    private fun buildTaskSummary(tasks: List<Task>): String {
        val completedTasks = tasks.filter { it.status.name == "DONE" }
        val inProgressTasks = tasks.filter { it.status.name == "IN_PROGRESS" }
        val todoTasks = tasks.filter { it.status.name == "TODO" }

        return buildString {
            if (completedTasks.isNotEmpty()) {
                appendLine("완료된 할일:")
                completedTasks.forEach { task ->
                    appendLine("- ${task.title.value}")
                }
            }

            if (inProgressTasks.isNotEmpty()) {
                appendLine("\n진행 중인 할일:")
                inProgressTasks.forEach { task ->
                    appendLine("- ${task.title.value}")
                }
            }

            if (todoTasks.isNotEmpty()) {
                appendLine("\n시작하지 않은 할일:")
                todoTasks.forEach { task ->
                    appendLine("- ${task.title.value}")
                }
            }

            appendLine("\n총 ${tasks.size}개의 할일 중 ${completedTasks.size}개 완료")
        }
    }

    private fun parseQuestionsFromResponse(
        response: String,
        expectedCount: Int,
    ): List<String> =
        try {
            val questions: List<String> =
                objectMapper.readValue(
                    response,
                    object : TypeReference<List<String>>() {},
                )
            questions.take(expectedCount)
        } catch (e: Exception) {
            logger.warn("JSON 파싱 실패, 텍스트 파싱 시도: ${e.message}")
            parseQuestionsFromText(response, expectedCount)
        }

    private fun parseQuestionsFromText(
        response: String,
        expectedCount: Int,
    ): List<String> {
        val lines =
            response
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    line
                        .removePrefix("-")
                        .removePrefix("•")
                        .trim()
                        .let { if (it.matches(Regex("^\\d+\\..*"))) it.substringAfter(".").trim() else it }
                }.filter { it.endsWith("?") || it.endsWith("?") }

        return lines.take(expectedCount)
    }

    private fun generateFallbackQuestions(
        tasks: List<Task>,
        questionCount: QuestionCount,
    ): List<String> {
        val fallbackQuestions = mutableListOf<String>()

        if (tasks.isNotEmpty()) {
            val completedTasks = tasks.filter { it.status.name == "DONE" }
            val inProgressTasks = tasks.filter { it.status.name == "IN_PROGRESS" }

            if (completedTasks.isNotEmpty()) {
                fallbackQuestions.add("완료한 '${completedTasks.first().title.value}' 작업에서 얻은 가장 큰 배움은 무엇인가요?")
            }
            if (inProgressTasks.isNotEmpty()) {
                fallbackQuestions.add("진행 중인 '${inProgressTasks.first().title.value}' 작업을 완료하기 위해 필요한 것은 무엇인가요?")
            }
        }

        val defaultQuestions =
            listOf(
                "이번 주 가장 잘한 일은 무엇인가요?",
                "이번 주 가장 어려웠던 점은 무엇인가요?",
                "다음 주에 개선하고 싶은 점은 무엇인가요?",
                "이번 주 배운 점이 있다면 무엇인가요?",
                "자신에게 칭찬해주고 싶은 점이 있나요?",
            )

        val remaining = questionCount.value - fallbackQuestions.size
        if (remaining > 0) {
            fallbackQuestions.addAll(defaultQuestions.take(remaining))
        }

        return fallbackQuestions.take(questionCount.value)
    }
}
