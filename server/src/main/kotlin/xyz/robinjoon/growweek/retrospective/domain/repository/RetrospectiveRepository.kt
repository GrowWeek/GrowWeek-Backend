package xyz.robinjoon.growweek.retrospective.domain.repository

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.retrospective.domain.model.Retrospective
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.model.query.RetrospectiveQuery

/**
 * Retrospective Repository 인터페이스
 *
 * Domain Layer의 Repository 인터페이스로,
 * Infrastructure Layer에서 구현됩니다.
 */
interface RetrospectiveRepository {
    /**
     * 여러 Retrospective Command를 처리하여 Retrospective 목록을 반환합니다.
     *
     * @param commands Retrospective Command 목록
     * @return 처리된 Retrospective 목록
     */
    fun saveAll(commands: List<RetrospectiveCommand>): List<Retrospective>

    /**
     * Query 조건에 맞는 Retrospective 목록을 페이징하여 반환합니다.
     *
     * @param query Retrospective Query
     * @return 페이징된 Retrospective 목록
     */
    fun findAll(query: RetrospectiveQuery): Page<Retrospective>
}
