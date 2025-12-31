package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Gemini API 설정 클래스
 *
 * GeminiProperties를 활성화합니다.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties::class)
class GeminiConfig
