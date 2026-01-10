package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.usecase.CreateRetrospectiveUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class CreateRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository,
) : CreateRetrospectiveUseCase {
    @Transactional
    override fun execute(command: RetrospectiveApplicationCommand.CreateRetrospective): RetrospectiveDto {
        val weekId = WeekId.of(command.startDate)

        val domainCommand =
            RetrospectiveCommand.CreateRetrospective(
                memberId = command.memberId,
                weekId = weekId,
                questionCount = QuestionCount(command.questionCount),
            )

        val savedRetrospectives = retrospectiveRepository.saveAll(listOf(domainCommand))
        val created = savedRetrospectives.first()

        return RetrospectiveDto.from(created)
    }
}
