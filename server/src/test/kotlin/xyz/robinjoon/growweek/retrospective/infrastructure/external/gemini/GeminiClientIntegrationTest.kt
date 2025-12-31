package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

/**
 * Gemini API 통합 테스트
 *
 * 실제 Gemini API를 호출하여 연동이 정상적으로 동작하는지 확인합니다.
 * 환경변수 GEMINI_API_KEY가 설정되어 있어야 실행됩니다.
 *
 * 주의: thinkingLevel은 gemini-3-flash-preview, gemini-3-pro-preview 모델에서만 지원됩니다.
 */
@EnabledIf(GeminiApiKeyCondition::class)
class GeminiClientIntegrationTest : BehaviorSpec({

    val apiKey = System.getenv("GEMINI_API_KEY") ?: ""

    Given("Gemini API 키가 설정되어 있을 때") {

        When("gemini-3-flash-preview 모델로 텍스트 생성을 요청하면") {
            val properties = GeminiProperties(
                apiKey = apiKey,
                model = "gemini-3-flash-preview",
                thinkingLevel = "low"
            )
            val client = GeminiClient(properties)

            val result = client.generateContent("1 + 1 = ? 숫자만 답해주세요.")

            Then("응답이 반환되어야 한다") {
                result.shouldNotBeBlank()
                println("gemini-3-flash-preview 응답: $result")
            }
        }

        When("gemini-3-pro-preview 모델로 텍스트 생성을 요청하면") {
            val properties = GeminiProperties(
                apiKey = apiKey,
                model = "gemini-3-pro-preview",
                thinkingLevel = "low"
            )
            val client = GeminiClient(properties)

            val result = client.generateContent("대한민국의 수도는? 도시 이름만 답해주세요.")

            Then("응답이 반환되어야 한다") {
                result.shouldNotBeBlank()
                println("gemini-3-pro-preview 응답: $result")
            }
        }

        When("JSON 배열 형식으로 질문 생성을 요청하면") {
            val properties = GeminiProperties(
                apiKey = apiKey,
                model = "gemini-3-flash-preview",
                thinkingLevel = "low"
            )
            val client = GeminiClient(properties)

            val prompt = """
                회고 질문 3개를 생성해주세요.
                JSON 배열 형식으로만 응답하세요.
                예시: ["질문1", "질문2", "질문3"]
            """.trimIndent()

            val result = client.generateContent(prompt)

            Then("JSON 배열 형식의 응답이 반환되어야 한다") {
                result.shouldNotBeBlank()
                result.contains("[") shouldBe true
                result.contains("]") shouldBe true
                println("질문 생성 응답: $result")
            }
        }
    }
})

/**
 * GEMINI_API_KEY 환경변수가 설정되어 있는지 확인하는 조건
 */
class GeminiApiKeyCondition : io.kotest.core.annotation.Condition {
    override fun evaluate(kclass: kotlin.reflect.KClass<out io.kotest.core.spec.Spec>): Boolean {
        val apiKey = System.getenv("GEMINI_API_KEY")
        return !apiKey.isNullOrBlank()
    }
}
