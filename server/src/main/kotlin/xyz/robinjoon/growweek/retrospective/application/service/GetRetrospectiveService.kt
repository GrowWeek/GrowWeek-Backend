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
            is RetrospectiveApplicationQuery.CursorByMemberId ->
                RetrospectiveQuery.Cursor.byMemberId(
                    memberId = query.memberId,
                    cursor = query.pageInfo.cursor,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.CursorByMemberIdAndPeriod ->
                RetrospectiveQuery.Cursor.byMemberIdAndPeriod(
                    memberId = query.memberId,
                    startDate = query.startDate,
                    endDate = query.endDate,
                    cursor = query.pageInfo.cursor,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.CursorByMemberIdAndMonth ->
                RetrospectiveQuery.Cursor.byMemberIdAndMonth(
                    memberId = query.memberId,
                    year = query.year,
                    month = query.month,
                    cursor = query.pageInfo.cursor,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.OffsetByMemberId ->
                RetrospectiveQuery.Offset.byMemberId(
                    memberId = query.memberId,
                    page = query.pageInfo.page,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.OffsetByMemberIdAndPeriod ->
                RetrospectiveQuery.Offset.byMemberIdAndPeriod(
                    memberId = query.memberId,
                    startDate = query.startDate,
                    endDate = query.endDate,
                    page = query.pageInfo.page,
                    size = query.pageInfo.size,
                    orderBy = query.pageInfo.orderBy
                )
            is RetrospectiveApplicationQuery.OffsetByMemberIdAndMonth ->
                RetrospectiveQuery.Offset.byMemberIdAndMonth(
                    memberId = query.memberId,
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
