package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.event.DomainEventPublisher
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.usecase.CompleteRetrospectiveUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class CompleteRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val eventPublisher: DomainEventPublisher
) : CompleteRetrospectiveUseCase {

    @Transactional
    override fun execute(command: RetrospectiveApplicationCommand.CompleteRetrospective): RetrospectiveDto {
        val domainCommand = RetrospectiveCommand.CompleteRetrospective(
            retrospectiveId = command.retrospectiveId,
            userId = command.userId
        )

        val savedRetrospectives = retrospectiveRepository.saveAll(listOf(domainCommand))
        val completed = savedRetrospectives.first()

        // 회고 완료 이벤트 발행
        eventPublisher.publish(
            RetrospectiveEventPayload.Completed(
                retrospectiveId = completed.id,
                userId = completed.userId,
                startDate = completed.period.startDate,
                endDate = completed.period.endDate
            )
        )

        return RetrospectiveDto.from(completed)
    }
}
