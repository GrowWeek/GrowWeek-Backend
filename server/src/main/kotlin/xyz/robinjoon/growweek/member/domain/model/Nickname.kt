package xyz.robinjoon.growweek.member.domain.model

@JvmInline
value class Nickname(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "닉네임은 비어있을 수 없습니다" }
        require(value.length <= 50) { "닉네임은 50자 이하여야 합니다" }
    }
}
