package xyz.robinjoon.growweek.member.application.command

sealed interface MemberApplicationCommand {
    data class SignUp(
        val email: String,
        val password: String,
        val nickname: String,
    ) : MemberApplicationCommand

    data class Login(
        val email: String,
        val password: String,
    ) : MemberApplicationCommand
}
