package xyz.robinjoon.growweek.task.domain.repository

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.task.domain.model.CompletedWeek
import xyz.robinjoon.growweek.task.domain.model.command.CompletedWeekCommand
import xyz.robinjoon.growweek.task.domain.model.query.CompletedWeekQuery

/**
 * 완료된 회고 주 Repository 인터페이스
 *
 * Domain Layer의 Repository 인터페이스로,
 * Infrastructure Layer에서 구현됩니다.
 */
interface CompletedWeekRepository {
    /**
     * 완료된 회고 주 Command를 처리합니다.
     *
     * @param commands Command 목록
     * @return 처리된 CompletedWeek 목록
     */
    fun saveAll(commands: List<CompletedWeekCommand>): List<CompletedWeek>

    /**
     * Query 조건에 맞는 완료된 회고 주를 조회합니다.
     *
     * @param query Query
     * @return 페이징된 CompletedWeek 목록
     */
    fun findAll(query: CompletedWeekQuery): Page<CompletedWeek>
}
