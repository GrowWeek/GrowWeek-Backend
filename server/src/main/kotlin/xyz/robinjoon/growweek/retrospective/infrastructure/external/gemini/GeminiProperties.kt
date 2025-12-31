package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * Gemini API 설정 프로퍼티
 *
 * application.yaml의 gemini 설정을 바인딩합니다.
 */
@ConfigurationProperties(prefix = "gemini")
data class GeminiProperties @ConstructorBinding constructor(
    val apiKey: String,
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val model: String = "gemini-2.0-flash",
    val thinkingLevel: String = "low"
)
