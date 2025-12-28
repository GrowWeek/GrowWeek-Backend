package xyz.robinjoon.growweek.member.domain.model.query

import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.Email

sealed class MemberQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    data class ById(
        val memberId: MemberId,
        override val pageInfo: PageInfo = DEFAULT_PAGE_INFO
    ) : MemberQuery(pageInfo)

    data class ByEmail(
        val email: Email,
        override val pageInfo: PageInfo = DEFAULT_PAGE_INFO
    ) : MemberQuery(pageInfo)

    companion object {
        private val DEFAULT_PAGE_INFO = OffsetPageInfo(page = 0, size = 1)

        fun byId(memberId: MemberId): ById = ById(memberId)

        fun byEmail(email: Email): ByEmail = ByEmail(email)
    }
}
