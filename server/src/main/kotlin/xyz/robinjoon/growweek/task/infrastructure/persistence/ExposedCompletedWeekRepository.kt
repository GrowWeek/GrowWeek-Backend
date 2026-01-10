package xyz.robinjoon.growweek.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.task.domain.model.CompletedWeek
import xyz.robinjoon.growweek.task.domain.model.command.CompletedWeekCommand
import xyz.robinjoon.growweek.task.domain.model.query.CompletedWeekQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedWeekRepository

@Repository
class ExposedCompletedWeekRepository : CompletedWeekRepository {
    @Transactional
    override fun saveAll(commands: List<CompletedWeekCommand>): List<CompletedWeek> {
        val savedWeeks = mutableListOf<CompletedWeek>()

        commands.forEach { command ->
            when (command) {
                is CompletedWeekCommand.Save -> {
                    CompletedWeekTable.insert {
                        it[retrospectiveId] = command.retrospectiveId.value
                        it[memberId] = command.memberId.value
                        it[weekId] = command.weekId.value
                        it[completedAt] = command.completedAt
                    }

                    savedWeeks.add(
                        CompletedWeek(
                            retrospectiveId = command.retrospectiveId,
                            memberId = command.memberId,
                            weekId = command.weekId,
                            completedAt = command.completedAt,
                        ),
                    )
                }

                is CompletedWeekCommand.Delete -> {
                    CompletedWeekTable.deleteWhere {
                        CompletedWeekTable.retrospectiveId eq command.retrospectiveId.value
                    }
                }
            }
        }

        return savedWeeks
    }

    @Transactional(readOnly = true)
    override fun findAll(query: CompletedWeekQuery): Page<CompletedWeek> {
        val pageInfo = query.pageInfo as OffsetPageInfo
        var baseQuery = CompletedWeekTable.selectAll()

        when (query) {
            is CompletedWeekQuery.OffsetByMemberIdAndWeekId -> {
                baseQuery =
                    baseQuery.andWhere {
                        (CompletedWeekTable.memberId eq query.memberId.value) and
                            (CompletedWeekTable.weekId eq query.weekId.value)
                    }
            }

            is CompletedWeekQuery.OffsetByRetrospectiveId -> {
                baseQuery =
                    baseQuery.andWhere {
                        CompletedWeekTable.retrospectiveId eq query.retrospectiveId.value
                    }
            }
        }

        // 정렬
        baseQuery =
            when (pageInfo.orderBy) {
                "completedAt" -> baseQuery.orderBy(CompletedWeekTable.completedAt to SortOrder.DESC)
                else -> baseQuery.orderBy(CompletedWeekTable.completedAt to SortOrder.DESC)
            }

        // 전체 개수
        val totalCount = baseQuery.count().toInt()
        val totalPage = if (totalCount == 0) 0 else (totalCount - 1) / pageInfo.size + 1

        // 페이징
        val items =
            baseQuery
                .limit(pageInfo.size)
                .offset((pageInfo.page * pageInfo.size).toLong())
                .map { it.toCompletedWeek() }

        return OffsetPage(
            items = items,
            page = pageInfo.page,
            size = pageInfo.size,
            totalPage = totalPage,
        )
    }

    private fun ResultRow.toCompletedWeek(): CompletedWeek =
        CompletedWeek(
            retrospectiveId = RetrospectiveId(this[CompletedWeekTable.retrospectiveId]),
            memberId = MemberId(this[CompletedWeekTable.memberId]),
            weekId = WeekId(this[CompletedWeekTable.weekId]),
            completedAt = this[CompletedWeekTable.completedAt],
        )
}
