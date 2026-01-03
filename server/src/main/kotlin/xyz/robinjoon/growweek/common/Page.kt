package xyz.robinjoon.growweek.common

sealed class Page<T>(
    val items: List<T>,
)

class CursorPage<T>(
    val cursor: String?,
    val nextCursor: String?,
    val size: Int,
    val hasNext: Boolean,
    items: List<T>,
) : Page<T>(items)

class OffsetPage<T>(
    val size: Int,
    val page: Int,
    val totalPage: Int,
    items: List<T>,
) : Page<T>(items)

interface PageQuery {
    val pageInfo: PageInfo
}

sealed interface PageInfo

data class CursorPageInfo(
    val cursor: String? = null,
    val size: Int = 20,
    val orderBy: String? = "createdAt",
) : PageInfo {
    init {
        require(size > 0) { "Size must be greater than 0" }
    }
}

data class OffsetPageInfo(
    val page: Int = 0,
    val size: Int = 20,
    val orderBy: String? = "createdAt",
) : PageInfo {
    init {
        require(page >= 0) { "Page must be non-negative" }
        require(size > 0) { "Size must be greater than 0" }
    }
}
