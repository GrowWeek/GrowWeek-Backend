package xyz.robinjoon.growweek.member.domain.model.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.Email

sealed class MemberQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    object Cursor {
        fun byEmail(
            email: Email,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): CursorByEmail {
            return CursorByEmail(
                email = email,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byMemberIds(
            memberIds: List<MemberId>,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): CursorByMemberIds {
            return CursorByMemberIds(
                memberIds = memberIds,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    object Offset {
        fun byEmail(
            email: Email,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): OffsetByEmail {
            return OffsetByEmail(
                email = email,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byMemberIds(
            memberIds: List<MemberId>,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): OffsetByMemberIds {
            return OffsetByMemberIds(
                memberIds = memberIds,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    data class CursorByEmail(
        val email: Email,
        override val pageInfo: CursorPageInfo
    ) : MemberQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByMemberIds(
        val memberIds: List<MemberId>,
        override val pageInfo: CursorPageInfo
    ) : MemberQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByEmail(
        val email: Email,
        override val pageInfo: OffsetPageInfo
    ) : MemberQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberIds(
        val memberIds: List<MemberId>,
        override val pageInfo: OffsetPageInfo
    ) : MemberQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
