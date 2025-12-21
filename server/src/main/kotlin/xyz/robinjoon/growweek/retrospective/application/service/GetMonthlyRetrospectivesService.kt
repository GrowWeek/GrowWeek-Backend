package xyz.robinjoon.growweek.retrospective.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.retrospective.application.dto.MonthlyRetrospectiveDto
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveStatisticsDto
import xyz.robinjoon.growweek.retrospective.application.dto.RetrospectiveSummaryDto
import xyz.robinjoon.growweek.retrospective.application.query.RetrospectiveApplicationQuery
import xyz.robinjoon.growweek.retrospective.application.usecase.GetMonthlyRetrospectivesUseCase
import xyz.robinjoon.growweek.retrospective.domain.model.RetrospectiveStatus
import xyz.robinjoon.growweek.retrospective.domain.model.query.RetrospectiveQuery
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

@Service
class GetMonthlyRetrospectivesService(
    private val retrospectiveRepository: RetrospectiveRepository
) : GetMonthlyRetrospectivesUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: RetrospectiveApplicationQuery.OffsetByUserIdAndMonth): MonthlyRetrospectiveDto {
        val domainQuery = RetrospectiveQuery.Offset.byUserIdAndMonth(
            userId = query.userId,
            year = query.year,
            month = query.month,
            page = query.pageInfo.page,
            size = query.pageInfo.size,
            orderBy = query.pageInfo.orderBy
        )

        val result = retrospectiveRepository.findAll(domainQuery)
        val retrospectives = result.items

        val statistics = RetrospectiveStatisticsDto(
            total = retrospectives.size,
            completed = retrospectives.count { it.status == RetrospectiveStatus.DONE },
            inProgress = retrospectives.count { it.status == RetrospectiveStatus.IN_PROGRESS },
            notStarted = retrospectives.count {
                it.status in listOf(
                    RetrospectiveStatus.TODO,
                    RetrospectiveStatus.BEFORE_GENERATE_QUESTION,
                    RetrospectiveStatus.AFTER_GENERATE_QUESTION
                )
            }
        )

        return MonthlyRetrospectiveDto(
            year = query.year,
            month = query.month,
            retrospectives = retrospectives.map { RetrospectiveSummaryDto.from(it) },
            statistics = statistics
        )
    }
}
