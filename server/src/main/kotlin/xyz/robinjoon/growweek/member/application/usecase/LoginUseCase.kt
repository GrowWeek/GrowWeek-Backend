package xyz.robinjoon.growweek.member.application.usecase

import xyz.robinjoon.growweek.member.application.command.MemberApplicationCommand
import xyz.robinjoon.growweek.member.application.dto.TokenDto

interface LoginUseCase {
    fun login(command: MemberApplicationCommand.Login): TokenDto
}
