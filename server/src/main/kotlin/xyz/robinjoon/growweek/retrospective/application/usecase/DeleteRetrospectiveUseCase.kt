package xyz.robinjoon.growweek.retrospective.application.usecase

import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand

interface DeleteRetrospectiveUseCase {
    fun execute(command: RetrospectiveApplicationCommand.DeleteRetrospective)
}
