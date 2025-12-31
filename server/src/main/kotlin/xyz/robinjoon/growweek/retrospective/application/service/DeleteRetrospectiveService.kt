package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.usecase.DeleteRetrospectiveUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class DeleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository
) : DeleteRetrospectiveUseCase {

    @Transactional
    override fun execute(command: RetrospectiveApplicationCommand.DeleteRetrospective) {
        val domainCommand = RetrospectiveCommand.DeleteRetrospective(
            retrospectiveId = command.retrospectiveId,
            memberId = command.memberId
        )

        retrospectiveRepository.saveAll(listOf(domainCommand))
    }
}
