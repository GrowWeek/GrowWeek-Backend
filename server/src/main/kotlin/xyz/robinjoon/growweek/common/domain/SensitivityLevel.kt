package xyz.robinjoon.growweek.common.domain

enum class SensitivityLevel {
    NONE,        // 민감하지 않음 - 모든 정보 AI에 전달
    TITLE_ONLY,  // 제목만 전달 - 제목만 AI에 전달
    NEVER        // 전달하지 않음 - AI에 전달하지 않음
}
