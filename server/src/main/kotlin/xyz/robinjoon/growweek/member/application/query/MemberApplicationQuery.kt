package xyz.robinjoon.growweek.member.application.query

import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId

sealed class MemberApplicationQuery(
    override val pageInfo: PageInfo,
) : PageQuery {
    data class ById(
        val memberId: MemberId,
        override val pageInfo: PageInfo = DEFAULT_PAGE_INFO,
    ) : MemberApplicationQuery(pageInfo)

    data class ByEmail(
        val email: String,
        override val pageInfo: PageInfo = DEFAULT_PAGE_INFO,
    ) : MemberApplicationQuery(pageInfo)

    companion object {
        private val DEFAULT_PAGE_INFO = OffsetPageInfo(page = 0, size = 1)

        fun byId(memberId: MemberId): ById = ById(memberId)

        fun byEmail(email: String): ByEmail = ByEmail(email)
    }
}
