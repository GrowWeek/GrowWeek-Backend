package xyz.robinjoon.growweek.retrospective.domain.model

enum class RetrospectiveStatus {
    TODO, // 최초 상태
    BEFORE_GENERATE_QUESTION, // 질문 생성 전
    AFTER_GENERATE_QUESTION, // 질문 생성 후, 답변 작성 전
    IN_PROGRESS, // 질문 생성 후, 답변 작성 중
    DONE, // 회고 완료
}
