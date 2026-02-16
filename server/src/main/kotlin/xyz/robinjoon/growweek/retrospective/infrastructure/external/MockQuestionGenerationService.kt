package xyz.robinjoon.growweek.retrospective.infrastructure.external

import org.springframework.stereotype.Service
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.retrospective.domain.model.RetrospectiveTask
import xyz.robinjoon.growweek.retrospective.domain.service.QuestionGenerationService

/**
 * Mock 질문 생성 서비스
 *
 * 실제 AI 연동 전 테스트용 Mock 구현체입니다.
 * 추후 OpenAI API 연동 시 OpenAIQuestionGenerationService로 교체합니다.
 */
@Service
class MockQuestionGenerationService : QuestionGenerationService {
    private val defaultQuestions =
        listOf(
            "이번 주 가장 잘한 일은 무엇인가요?",
            "이번 주 가장 어려웠던 점은 무엇인가요?",
            "다음 주에 개선하고 싶은 점은 무엇인가요?",
            "이번 주 배운 점이 있다면 무엇인가요?",
            "팀원들에게 공유하고 싶은 인사이트가 있나요?",
            "이번 주 목표 달성도는 어떠했나요?",
            "자신에게 칭찬해주고 싶은 점이 있나요?",
        )

    override suspend fun generateQuestions(
        tasks: List<RetrospectiveTask>,
        questionCount: QuestionCount,
    ): List<String> {
        val questions = mutableListOf<String>()

        if (tasks.isNotEmpty()) {
            val completedTasks = tasks.filter { it.status == "DONE" }
            val inProgressTasks = tasks.filter { it.status == "IN_PROGRESS" }

            if (completedTasks.isNotEmpty()) {
                questions.add("완료한 '${completedTasks.first().title}' 작업에서 얻은 인사이트는 무엇인가요?")
            }
            if (inProgressTasks.isNotEmpty()) {
                questions.add("진행 중인 '${inProgressTasks.first().title}' 작업에서 어려운 점이 있나요?")
            }
        }

        val remaining = questionCount.value - questions.size
        if (remaining > 0) {
            questions.addAll(defaultQuestions.take(remaining))
        }

        return questions.take(questionCount.value)
    }
}
