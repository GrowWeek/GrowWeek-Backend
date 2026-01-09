package xyz.robinjoon.growweek.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.task.domain.model.CompletedRetrospectivePeriod
import xyz.robinjoon.growweek.task.domain.model.command.CompletedRetrospectivePeriodCommand
import xyz.robinjoon.growweek.task.domain.model.query.CompletedRetrospectivePeriodQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedRetrospectivePeriodRepository

@Repository
class ExposedCompletedRetrospectivePeriodRepository : CompletedRetrospectivePeriodRepository {
    @Transactional
    override fun saveAll(commands: List<CompletedRetrospectivePeriodCommand>): List<CompletedRetrospectivePeriod> {
        val savedPeriods = mutableListOf<CompletedRetrospectivePeriod>()

        commands.forEach { command ->
            when (command) {
                is CompletedRetrospectivePeriodCommand.Save -> {
                    CompletedRetrospectivePeriodTable.insert {
                        it[retrospectiveId] = command.retrospectiveId.value
                        it[memberId] = command.memberId.value
                        it[startDate] = command.startDate
                        it[endDate] = command.endDate
                        it[completedAt] = command.completedAt
                    }

                    savedPeriods.add(
                        CompletedRetrospectivePeriod(
                            retrospectiveId = command.retrospectiveId,
                            memberId = command.memberId,
                            startDate = command.startDate,
                            endDate = command.endDate,
                            completedAt = command.completedAt,
                        ),
                    )
                }

                is CompletedRetrospectivePeriodCommand.Delete -> {
                    CompletedRetrospectivePeriodTable.deleteWhere {
                        CompletedRetrospectivePeriodTable.retrospectiveId eq command.retrospectiveId.value
                    }
                }
            }
        }

        return savedPeriods
    }

    @Transactional(readOnly = true)
    override fun findAll(query: CompletedRetrospectivePeriodQuery): Page<CompletedRetrospectivePeriod> {
        val pageInfo = query.pageInfo as OffsetPageInfo
        var baseQuery = CompletedRetrospectivePeriodTable.selectAll()

        when (query) {
            is CompletedRetrospectivePeriodQuery.OffsetByMemberIdAndOverlappingPeriod -> {
                baseQuery =
                    baseQuery.andWhere {
                        (CompletedRetrospectivePeriodTable.memberId eq query.memberId.value) and
                            (CompletedRetrospectivePeriodTable.startDate lessEq query.periodEnd) and
                            (CompletedRetrospectivePeriodTable.endDate greaterEq query.periodStart)
                    }
            }

            is CompletedRetrospectivePeriodQuery.OffsetByRetrospectiveId -> {
                baseQuery =
                    baseQuery.andWhere {
                        CompletedRetrospectivePeriodTable.retrospectiveId eq query.retrospectiveId.value
                    }
            }
        }

        // 정렬
        baseQuery =
            when (pageInfo.orderBy) {
                "completedAt" -> baseQuery.orderBy(CompletedRetrospectivePeriodTable.completedAt to SortOrder.DESC)
                else -> baseQuery.orderBy(CompletedRetrospectivePeriodTable.completedAt to SortOrder.DESC)
            }

        // 전체 개수
        val totalCount = baseQuery.count().toInt()
        val totalPage = if (totalCount == 0) 0 else (totalCount - 1) / pageInfo.size + 1

        // 페이징
        val items =
            baseQuery
                .limit(pageInfo.size)
                .offset((pageInfo.page * pageInfo.size).toLong())
                .map { it.toCompletedRetrospectivePeriod() }

        return OffsetPage(
            items = items,
            page = pageInfo.page,
            size = pageInfo.size,
            totalPage = totalPage,
        )
    }

    private fun ResultRow.toCompletedRetrospectivePeriod(): CompletedRetrospectivePeriod =
        CompletedRetrospectivePeriod(
            retrospectiveId = RetrospectiveId(this[CompletedRetrospectivePeriodTable.retrospectiveId]),
            memberId = MemberId(this[CompletedRetrospectivePeriodTable.memberId]),
            startDate = this[CompletedRetrospectivePeriodTable.startDate],
            endDate = this[CompletedRetrospectivePeriodTable.endDate],
            completedAt = this[CompletedRetrospectivePeriodTable.completedAt],
        )
}
