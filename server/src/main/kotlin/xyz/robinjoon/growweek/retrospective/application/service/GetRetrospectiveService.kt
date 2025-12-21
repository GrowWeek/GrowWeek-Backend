package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.CursorPage
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveSummaryDto
import xyz.robinjoon.growweek.retrospective.application.query.RetrospectiveApplicationQuery
import xyz.robinjoon.growweek.retrospective.application.usecase.GetRetrospectiveUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.query.RetrospectiveQuery
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class GetRetrospectiveService(
    private val retrospectiveRepository: RetrospectiveRepository
) : GetRetrospectiveUseCase {

    @Transactional(readOnly = true)
    override fun getById(query: RetrospectiveApplicationQuery.ByRetrospectiveId): RetrospectiveDto {
        val domainQuery = RetrospectiveQuery.Offset.byRetrospectiveId(query.retrospectiveId)
        val result = retrospectiveRepository.findAll(domainQuery)
        val retrospective = result.items.firstOrNull()
            ?: throw IllegalArgumentException("Retrospective not found: ${query.retrospectiveId.value}")

        return RetrospectiveDto.from(retrospective)
    }

    @Transactional(readOnly = true)
    override fun getList(query: RetrospectiveApplicationQuery): Page<RetrospectiveSummaryDto> {
        val domainQuery = toDomainQuery(query)
        val result = retrospectiveRepository.findAll(domainQuery)

        return when (result) {
            is CursorPage -> CursorPage(
                items = result.items.map { RetrospectiveSummaryDto.from(it) },
                cursor = result.cursor,
                size = result.size,
                nextCursor = result.nextCursor,
                hasNext = result.hasNext
            )
            is OffsetPage -> OffsetPage(
                items = result.items.map { RetrospectiveSummaryDto.from(it) },
                page = result.page,
                size = result.size,
                totalPage = result.totalPage
            )
            else -> throw IllegalStateException("Unsupported page type")
        }
    }

    private fun toDomainQuery(query: RetrospectiveApplicationQuery): RetrospectiveQuery {
        return when (query) {
            is RetrospectiveApplicationQuery.CursorByUserId ->
                RetrospectiveQuery.Cursor.byUserId(
                    userId = query.userId,
                    cursor = query.pageInfo.cursor,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.CursorByUserIdAndPeriod ->
                RetrospectiveQuery.Cursor.byUserIdAndPeriod(
                    userId = query.userId,
                    startDate = query.startDate,
                    endDate = query.endDate,
                    cursor = query.pageInfo.cursor,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.CursorByUserIdAndMonth ->
                RetrospectiveQuery.Cursor.byUserIdAndMonth(
                    userId = query.userId,
                    year = query.year,
                    month = query.month,
                    cursor = query.pageInfo.cursor,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.OffsetByUserId ->
                RetrospectiveQuery.Offset.byUserId(
                    userId = query.userId,
                    page = query.pageInfo.page,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.OffsetByUserIdAndPeriod ->
                RetrospectiveQuery.Offset.byUserIdAndPeriod(
                    userId = query.userId,
                    startDate = query.startDate,
                    endDate = query.endDate,
                    page = query.pageInfo.page,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.OffsetByUserIdAndMonth ->
                RetrospectiveQuery.Offset.byUserIdAndMonth(
                    userId = query.userId,
                    year = query.year,
                    month = query.month,
                    page = query.pageInfo.page,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.ByRetrospectiveId ->
                RetrospectiveQuery.Offset.byRetrospectiveId(query.retrospectiveId)
        }
    }
}
