package xyz.robinjoon.growweek.member.application.dto

data class TokenDto(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)
