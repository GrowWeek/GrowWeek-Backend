package xyz.robinjoon.growweek.retrospective.application.usecase

import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto

interface CreateRetrospectiveUseCase {
    fun execute(command: RetrospectiveApplicationCommand.CreateRetrospective): RetrospectiveDto
}
