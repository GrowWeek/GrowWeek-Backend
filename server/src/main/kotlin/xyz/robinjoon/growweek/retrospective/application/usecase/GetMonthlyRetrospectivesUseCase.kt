package xyz.robinjoon.growweek.retrospective.application.usecase

import xyz.robinjoon.growweek.retrospective.application.dto.MonthlyRetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.query.RetrospectiveApplicationQuery

interface GetMonthlyRetrospectivesUseCase {
    fun execute(query: RetrospectiveApplicationQuery.OffsetByUserIdAndMonth): MonthlyRetrospectiveDto
}
