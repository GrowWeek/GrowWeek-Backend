package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.usecase.CompleteRetrospectiveUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class CompleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository
) : CompleteRetrospectiveUseCase {

    @Transactional
    override fun execute(command: RetrospectiveApplicationCommand.CompleteRetrospective): RetrospectiveDto {
        val domainCommand = RetrospectiveCommand.CompleteRetrospective(
            retrospectiveId = command.retrospectiveId,
            userId = command.userId
        )

        val savedRetrospectives = retrospectiveRepository.saveAll(listOf(domainCommand))
        return RetrospectiveDto.from(savedRetrospectives.first())
    }
}
