package xyz.robinjoon.growweek.member.domain.model

@JvmInline
value class RawPassword(val value: String) {
    init {
        require(value.length >= MIN_LENGTH) {
            "Password must be at least $MIN_LENGTH characters long"
        }
        require(value.length <= MAX_LENGTH) {
            "Password must be at most $MAX_LENGTH characters long"
        }
        require(value.any { it.isUpperCase() }) {
            "Password must contain at least one uppercase letter"
        }
        require(value.any { it.isLowerCase() }) {
            "Password must contain at least one lowercase letter"
        }
        require(value.any { it.isDigit() }) {
            "Password must contain at least one digit"
        }
        require(value.any { it in SPECIAL_CHARACTERS }) {
            "Password must contain at least one special character"
        }
    }

    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 128
        private const val SPECIAL_CHARACTERS = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
    }
}
