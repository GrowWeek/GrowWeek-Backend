package xyz.robinjoon.growweek.member.application.usecase

import xyz.robinjoon.growweek.member.application.command.MemberApplicationCommand
import xyz.robinjoon.growweek.member.application.dto.MemberDto

interface SignUpUseCase {
    fun signUp(command: MemberApplicationCommand.SignUp): MemberDto
}
