package xyz.robinjoon.growweek.member.domain.model.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.Email
import xyz.robinjoon.growweek.member.domain.model.EncodedPassword
import xyz.robinjoon.growweek.member.domain.model.MemberName

sealed interface MemberCommand {
    data class CreateMember(
        val email: Email,
        val password: EncodedPassword,
        val name: MemberName
    ) : MemberCommand

    data class UpdateMember(
        val memberId: MemberId,
        val name: MemberName? = null
    ) : MemberCommand

    data class UpdatePassword(
        val memberId: MemberId,
        val password: EncodedPassword
    ) : MemberCommand

    data class DeleteMember(
        val memberId: MemberId
    ) : MemberCommand
}
