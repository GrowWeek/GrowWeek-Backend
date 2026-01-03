package xyz.robinjoon.growweek.retrospective.domain.model

@JvmInline
value class AdditionalNotes(
    val value: String,
) {
    init {
        require(value.length <= MAX_LENGTH) {
            "기타 회고 내용은 ${MAX_LENGTH}자를 초과할 수 없습니다"
        }
    }

    companion object {
        private const val MAX_LENGTH = 3000
    }
}
