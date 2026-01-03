package xyz.robinjoon.growweek.member.domain.model

@JvmInline
value class MemberName(val value: String) {
    init {
        require(value.isNotBlank()) { "Member name must not be blank" }
        require(value.length <= MAX_LENGTH) { "Member name must be at most $MAX_LENGTH characters long" }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
