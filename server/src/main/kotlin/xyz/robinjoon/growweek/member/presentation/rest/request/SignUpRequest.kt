package xyz.robinjoon.growweek.member.presentation.rest.request

data class SignUpRequest(
    val email: String,
    val password: String,
    val nickname: String,
)
