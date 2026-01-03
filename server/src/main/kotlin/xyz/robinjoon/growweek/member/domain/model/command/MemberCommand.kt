package xyz.robinjoon.growweek.member.domain.model.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.Email
import xyz.robinjoon.growweek.member.domain.model.Nickname
import xyz.robinjoon.growweek.member.domain.model.Password

sealed interface MemberCommand {
    data class CreateMember(
        val email: Email,
        val password: Password,
        val nickname: Nickname,
    ) : MemberCommand

    data class UpdateMember(
        val memberId: MemberId,
        val nickname: Nickname?,
    ) : MemberCommand

    data class DeactivateMember(
        val memberId: MemberId,
    ) : MemberCommand
}
