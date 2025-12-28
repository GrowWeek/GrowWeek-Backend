package xyz.robinjoon.growweek.member.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.application.dto.MemberDto
import xyz.robinjoon.growweek.member.application.usecase.GetMemberUseCase
import xyz.robinjoon.growweek.member.domain.model.query.MemberQuery
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository

@Service
class GetMemberService(
    private val memberRepository: MemberRepository
) : GetMemberUseCase {

    @Transactional(readOnly = true)
    override fun getMember(memberId: MemberId): MemberDto? {
        val member = memberRepository.findAll(MemberQuery.byId(memberId)).items.firstOrNull()
            ?: return null

        return MemberDto.from(member)
    }
}
