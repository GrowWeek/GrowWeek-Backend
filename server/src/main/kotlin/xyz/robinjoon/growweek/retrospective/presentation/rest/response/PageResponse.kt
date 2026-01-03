package xyz.robinjoon.growweek.retrospective.presentation.rest.response

import xyz.robinjoon.growweek.common.CursorPage
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.Page

/**
 * 페이지 응답 (커서 또는 오프셋 기반)
 */
sealed class PageResponse<T> {
    /** 항목 목록 */
    abstract val items: List<T>
}

/**
 * 커서 기반 페이지 응답 DTO
 */
data class CursorPageResponse<T>(
    /** 현재 커서 */
    val cursor: String?,
    /** 다음 페이지 커서 (null이면 마지막 페이지) */
    val nextCursor: String?,
    /** 페이지 크기 */
    val size: Int,
    /** 다음 페이지 존재 여부 */
    val hasNext: Boolean,
    /** 항목 목록 */
    override val items: List<T>,
) : PageResponse<T>() {
    companion object {
        fun <T, R> from(
            page: CursorPage<T>,
            transform: (T) -> R,
        ): CursorPageResponse<R> =
            CursorPageResponse(
                cursor = page.cursor,
                nextCursor = page.nextCursor,
                size = page.size,
                hasNext = page.hasNext,
                items = page.items.map(transform),
            )
    }
}

/**
 * 오프셋 기반 페이지 응답 DTO
 */
data class OffsetPageResponse<T>(
    /** 현재 페이지 번호 (0부터 시작) */
    val page: Int,
    /** 페이지 크기 */
    val size: Int,
    /** 전체 페이지 수 */
    val totalPage: Int,
    /** 항목 목록 */
    override val items: List<T>,
) : PageResponse<T>() {
    companion object {
        fun <T, R> from(
            page: OffsetPage<T>,
            transform: (T) -> R,
        ): OffsetPageResponse<R> =
            OffsetPageResponse(
                page = page.page,
                size = page.size,
                totalPage = page.totalPage,
                items = page.items.map(transform),
            )
    }
}

fun <T, R> Page<T>.toResponse(transform: (T) -> R): PageResponse<R> =
    when (this) {
        is CursorPage -> CursorPageResponse.from(this, transform)
        is OffsetPage -> OffsetPageResponse.from(this, transform)
    }
