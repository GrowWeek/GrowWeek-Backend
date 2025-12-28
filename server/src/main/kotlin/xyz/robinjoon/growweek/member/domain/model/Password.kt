package xyz.robinjoon.growweek.member.domain.model

@JvmInline
value class Password(val value: String) {
    init {
        require(value.isNotBlank()) { "비밀번호는 비어있을 수 없습니다" }
    }

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 100

        fun validate(rawPassword: String) {
            require(rawPassword.length >= MIN_LENGTH) { "비밀번호는 ${MIN_LENGTH}자 이상이어야 합니다" }
            require(rawPassword.length <= MAX_LENGTH) { "비밀번호는 ${MAX_LENGTH}자 이하여야 합니다" }
        }
    }
}
