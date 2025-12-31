package xyz.robinjoon.growweek.member.domain.model

@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "이메일은 비어있을 수 없습니다" }
        require(value.length <= 100) { "이메일은 100자 이하여야 합니다" }
        require(EMAIL_REGEX.matches(value)) { "올바른 이메일 형식이 아닙니다" }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
