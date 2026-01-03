package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.usecase.GenerateQuestionsUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.model.query.RetrospectiveQuery
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import xyz.robinjoon.growweek.retrospective.domain.service.QuestionGenerationService
import xyz.robinjoon.growweek.task.domain.model.Task
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class GenerateQuestionsService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val taskRepository: TaskRepository,
    private val questionGenerationService: QuestionGenerationService,
) : GenerateQuestionsUseCase {
    @Transactional
    override suspend fun execute(command: RetrospectiveApplicationCommand.GenerateQuestions): RetrospectiveDto {
        // 1. 회고 조회
        val retrospective =
            retrospectiveRepository
                .findAll(
                    RetrospectiveQuery.Offset.byRetrospectiveId(command.retrospectiveId),
                ).items
                .firstOrNull()
                ?: throw IllegalArgumentException("Retrospective not found: ${command.retrospectiveId.value}")

        // 2. 질문 생성 시작 상태로 변경
        retrospectiveRepository.saveAll(
            listOf(
                RetrospectiveCommand.GenerateQuestions(
                    retrospectiveId = command.retrospectiveId,
                    memberId = command.memberId,
                ),
            ),
        )

        // 3. 해당 기간의 할일 목록 조회
        val tasksPage =
            taskRepository.findAll(
                TaskQuery.Offset.byMemberIdAndWeek(
                    memberId = command.memberId,
                    weekStart = retrospective.period.startDate,
                    weekEnd = retrospective.period.endDate,
                    size = 100,
                ),
            )

        // 4. 민감도에 따라 할일 데이터 필터링
        val filteredTasks = filterTasksBySensitivity(tasksPage.items)

        // 5. AI 질문 생성
        val generatedQuestions =
            questionGenerationService.generateQuestions(
                tasks = filteredTasks,
                questionCount = retrospective.questionCount,
            )

        // 6. 질문 생성 완료
        val savedRetrospectives =
            retrospectiveRepository.saveAll(
                listOf(
                    RetrospectiveCommand.CompleteQuestionGeneration(
                        retrospectiveId = command.retrospectiveId,
                        generatedQuestionContents = generatedQuestions,
                    ),
                ),
            )

        return RetrospectiveDto.from(savedRetrospectives.first())
    }

    private fun filterTasksBySensitivity(tasks: List<Task>): List<Task> =
        tasks.mapNotNull { task ->
            when (task.sensitivityLevel) {
                SensitivityLevel.NONE -> task
                SensitivityLevel.TITLE_ONLY -> task.copy(description = null)
                SensitivityLevel.NEVER -> null
            }
        }
}
