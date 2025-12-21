package xyz.robinjoon.growweek.retrospective.application.usecase

import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto

interface WriteAdditionalNotesUseCase {
    fun execute(command: RetrospectiveApplicationCommand.WriteAdditionalNotes): RetrospectiveDto
}
