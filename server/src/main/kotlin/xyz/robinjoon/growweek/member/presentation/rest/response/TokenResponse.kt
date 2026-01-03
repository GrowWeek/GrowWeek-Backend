package xyz.robinjoon.growweek.member.presentation.rest.response

import xyz.robinjoon.growweek.member.application.dto.TokenDto

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
) {
    companion object {
        fun from(dto: TokenDto): TokenResponse =
            TokenResponse(
                accessToken = dto.accessToken,
                tokenType = dto.tokenType,
                expiresIn = dto.expiresIn,
            )
    }
}
