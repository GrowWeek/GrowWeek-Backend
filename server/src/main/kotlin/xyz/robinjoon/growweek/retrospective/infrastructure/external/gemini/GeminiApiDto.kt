package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

/**
 * Gemini API 요청 DTO
 */
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
) {
    data class Content(
        val parts: List<Part>
    )

    data class Part(
        val text: String
    )

    data class GenerationConfig(
        val thinkingConfig: ThinkingConfig? = null,
        val responseMimeType: String? = null,
        val responseSchema: ResponseSchema? = null
    )

    data class ThinkingConfig(
        val thinkingLevel: String
    )

    data class ResponseSchema(
        val type: String,
        val items: SchemaItems? = null
    )

    data class SchemaItems(
        val type: String
    )

    companion object {
        fun createTextRequest(
            prompt: String,
            thinkingLevel: String = "low"
        ): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = thinkingLevel),
                    responseMimeType = "application/json",
                    responseSchema = ResponseSchema(
                        type = "array",
                        items = SchemaItems(type = "string")
                    )
                )
            )
        }
    }
}

/**
 * Gemini API 응답 DTO
 */
data class GeminiResponse(
    val candidates: List<Candidate>?,
    val usageMetadata: UsageMetadata? = null,
    val modelVersion: String? = null,
    val error: GeminiError? = null
) {
    data class Candidate(
        val content: Content?,
        val finishReason: String? = null,
        val index: Int? = null
    )

    data class Content(
        val parts: List<Part>?,
        val role: String? = null
    )

    data class Part(
        val text: String? = null,
        val thought: Boolean? = null
    )

    data class UsageMetadata(
        val promptTokenCount: Int? = null,
        val candidatesTokenCount: Int? = null,
        val totalTokenCount: Int? = null
    )

    data class GeminiError(
        val code: Int?,
        val message: String?,
        val status: String?
    )

    /**
     * 응답에서 텍스트 추출
     */
    fun extractText(): String? {
        return candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.filter { it.thought != true }
            ?.mapNotNull { it.text }
            ?.joinToString("")
    }
}
