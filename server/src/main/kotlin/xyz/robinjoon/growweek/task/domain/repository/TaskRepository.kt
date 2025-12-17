package xyz.robinjoon.growweek.task.domain.repository

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.task.domain.model.Task
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery

/**
 * Task Repository 인터페이스
 *
 * Domain Layer의 Repository 인터페이스로,
 * Infrastructure Layer에서 구현됩니다.
 */
interface TaskRepository {
    /**
     * 여러 Task Command를 처리하여 Task 목록을 반환합니다.
     *
     * @param commands Task Command 목록
     * @return 처리된 Task 목록
     */
    fun saveAll(commands: List<TaskCommand>): List<Task>

    /**
     * Query 조건에 맞는 Task 목록을 페이징하여 반환합니다.
     *
     * @param query Task Query
     * @return 페이징된 Task 목록
     */
    fun findAll(query: TaskQuery): Page<Task>
}
