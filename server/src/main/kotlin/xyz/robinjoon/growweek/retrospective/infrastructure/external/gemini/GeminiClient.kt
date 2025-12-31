package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Gemini API 클라이언트
 *
 * REST API를 통해 Google Gemini 모델과 통신합니다.
 */
@Component
class GeminiClient(
    private val geminiProperties: GeminiProperties
) {
    private val logger = LoggerFactory.getLogger(GeminiClient::class.java)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(geminiProperties.baseUrl)
        .defaultHeader("x-goog-api-key", geminiProperties.apiKey)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * Gemini API에 텍스트 생성 요청
     *
     * @param prompt 프롬프트 텍스트
     * @return 생성된 텍스트 응답
     * @throws GeminiApiException API 호출 실패 시
     */
    fun generateContent(prompt: String): String {
        val request = GeminiRequest.createTextRequest(
            prompt = prompt,
            thinkingLevel = geminiProperties.thinkingLevel
        )

        return try {
            val response = restClient.post()
                .uri("/v1beta/models/${geminiProperties.model}:generateContent")
                .body(request)
                .retrieve()
                .body(GeminiResponse::class.java)

            response?.let { geminiResponse ->
                geminiResponse.error?.let { error ->
                    logger.error("Gemini API error: ${error.message}")
                    throw GeminiApiException("Gemini API 오류: ${error.message}", error.code)
                }

                geminiResponse.extractText()
                    ?: throw GeminiApiException("Gemini API 응답에서 텍스트를 추출할 수 없습니다")
            } ?: throw GeminiApiException("Gemini API 응답이 비어있습니다")
        } catch (e: RestClientException) {
            logger.error("Gemini API 호출 실패", e)
            throw GeminiApiException("Gemini API 호출 실패: ${e.message}", cause = e)
        }
    }
}

/**
 * Gemini API 예외
 */
class GeminiApiException(
    message: String,
    val errorCode: Int? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)
