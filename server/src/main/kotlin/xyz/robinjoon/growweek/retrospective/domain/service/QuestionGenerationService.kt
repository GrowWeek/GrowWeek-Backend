package xyz.robinjoon.growweek.retrospective.domain.service

import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.task.domain.model.Task

/**
 * 회고 질문 생성 서비스 인터페이스
 *
 * Domain Layer의 Service 인터페이스로,
 * Infrastructure Layer에서 AI(OpenAI 등)를 활용하여 구현됩니다.
 */
interface QuestionGenerationService {
    /**
     * 주간 할일 데이터를 기반으로 회고 질문 생성
     *
     * @param tasks 주간 할일 목록 (민감도에 따라 필터링된 데이터)
     * @param questionCount 생성할 질문 개수
     * @return 생성된 질문 내용 목록
     */
    suspend fun generateQuestions(
        tasks: List<Task>,
        questionCount: QuestionCount,
    ): List<String>
}
