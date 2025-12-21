package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.usecase.WriteAnswerUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class WriteAnswerService(
    private val retrospectiveRepository: RetrospectiveRepository
) : WriteAnswerUseCase {

    @Transactional
    override fun execute(command: RetrospectiveApplicationCommand.WriteAnswer): RetrospectiveDto {
        val domainCommand = RetrospectiveCommand.WriteAnswer(
            retrospectiveId = command.retrospectiveId,
            userId = command.userId,
            questionId = command.questionId,
            content = command.content
        )

        val savedRetrospectives = retrospectiveRepository.saveAll(listOf(domainCommand))
        return RetrospectiveDto.from(savedRetrospectives.first())
    }
}
