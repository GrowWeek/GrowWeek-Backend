package xyz.robinjoon.growweek.task.domain.repository

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.task.domain.model.CompletedRetrospectivePeriod
import xyz.robinjoon.growweek.task.domain.model.command.CompletedRetrospectivePeriodCommand
import xyz.robinjoon.growweek.task.domain.model.query.CompletedRetrospectivePeriodQuery

/**
 * 완료된 회고 기간 Repository 인터페이스
 *
 * Domain Layer의 Repository 인터페이스로,
 * Infrastructure Layer에서 구현됩니다.
 */
interface CompletedRetrospectivePeriodRepository {
    /**
     * 완료된 회고 기간 Command를 처리합니다.
     *
     * @param commands Command 목록
     * @return 처리된 CompletedRetrospectivePeriod 목록
     */
    fun saveAll(commands: List<CompletedRetrospectivePeriodCommand>): List<CompletedRetrospectivePeriod>

    /**
     * Query 조건에 맞는 완료된 회고 기간을 조회합니다.
     *
     * @param query Query
     * @return 페이징된 CompletedRetrospectivePeriod 목록
     */
    fun findAll(query: CompletedRetrospectivePeriodQuery): Page<CompletedRetrospectivePeriod>
}
