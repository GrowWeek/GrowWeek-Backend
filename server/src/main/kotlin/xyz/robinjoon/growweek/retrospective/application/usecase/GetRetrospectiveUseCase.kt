package xyz.robinjoon.growweek.retrospective.application.usecase

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveSummaryDto
import xyz.robinjoon.growweek.retrospective.application.query.RetrospectiveApplicationQuery

interface GetRetrospectiveUseCase {
    fun getById(query: RetrospectiveApplicationQuery.ByRetrospectiveId): RetrospectiveDto

    fun getList(query: RetrospectiveApplicationQuery): Page<RetrospectiveSummaryDto>
}
