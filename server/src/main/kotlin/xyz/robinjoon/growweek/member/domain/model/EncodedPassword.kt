package xyz.robinjoon.growweek.member.domain.model

@JvmInline
value class EncodedPassword(val value: String) {
    init {
        require(value.isNotBlank()) { "Encoded password must not be blank" }
    }
}
