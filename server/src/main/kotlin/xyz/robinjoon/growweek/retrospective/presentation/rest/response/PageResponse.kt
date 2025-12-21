package xyz.robinjoon.growweek.retrospective.presentation.rest.response

import xyz.robinjoon.growweek.common.CursorPage
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.Page

sealed class PageResponse<T> {
    abstract val items: List<T>
}

data class CursorPageResponse<T>(
    val cursor: String?,
    val nextCursor: String?,
    val size: Int,
    val hasNext: Boolean,
    override val items: List<T>
) : PageResponse<T>() {
    companion object {
        fun <T, R> from(page: CursorPage<T>, transform: (T) -> R): CursorPageResponse<R> {
            return CursorPageResponse(
                cursor = page.cursor,
                nextCursor = page.nextCursor,
                size = page.size,
                hasNext = page.hasNext,
                items = page.items.map(transform)
            )
        }
    }
}

data class OffsetPageResponse<T>(
    val page: Int,
    val size: Int,
    val totalPage: Int,
    override val items: List<T>
) : PageResponse<T>() {
    companion object {
        fun <T, R> from(page: OffsetPage<T>, transform: (T) -> R): OffsetPageResponse<R> {
            return OffsetPageResponse(
                page = page.page,
                size = page.size,
                totalPage = page.totalPage,
                items = page.items.map(transform)
            )
        }
    }
}

fun <T, R> Page<T>.toResponse(transform: (T) -> R): PageResponse<R> {
    return when (this) {
        is CursorPage -> CursorPageResponse.from(this, transform)
        is OffsetPage -> OffsetPageResponse.from(this, transform)
    }
}
