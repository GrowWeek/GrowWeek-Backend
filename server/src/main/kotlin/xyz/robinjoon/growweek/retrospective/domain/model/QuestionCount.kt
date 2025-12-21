package xyz.robinjoon.growweek.retrospective.domain.model

@JvmInline
value class QuestionCount(val value: Int) {
    init {
        require(value in MIN_COUNT..MAX_COUNT) {
            "질문 개수는 최소 ${MIN_COUNT}개, 최대 ${MAX_COUNT}개여야 합니다"
        }
    }

    companion object {
        private const val MIN_COUNT = 2
        private const val MAX_COUNT = 7
        val DEFAULT = QuestionCount(3)
    }
}
