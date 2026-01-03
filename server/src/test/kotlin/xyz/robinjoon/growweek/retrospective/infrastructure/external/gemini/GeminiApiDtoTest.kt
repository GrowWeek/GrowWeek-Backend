package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GeminiApiDtoTest :
    BehaviorSpec({

        val objectMapper = jacksonObjectMapper()

        Given("GeminiRequest를 생성할 때") {

            When("텍스트 프롬프트로 요청을 생성하면") {
                val request =
                    GeminiRequest.createTextRequest(
                        prompt = "테스트 프롬프트",
                        thinkingLevel = "low",
                    )

                Then("contents가 올바르게 설정되어야 한다") {
                    request.contents.size shouldBe 1
                    request.contents[0].parts.size shouldBe 1
                    request.contents[0].parts[0].text shouldBe "테스트 프롬프트"
                }

                Then("generationConfig가 올바르게 설정되어야 한다") {
                    request.generationConfig shouldNotBe null
                    request.generationConfig?.thinkingConfig?.thinkingLevel shouldBe "low"
                    request.generationConfig?.responseMimeType shouldBe "application/json"
                }

                Then("responseSchema가 올바르게 설정되어야 한다") {
                    request.generationConfig?.responseSchema?.type shouldBe "array"
                    request.generationConfig
                        ?.responseSchema
                        ?.items
                        ?.type shouldBe "string"
                }
            }

            When("요청을 JSON으로 직렬화하면") {
                val request =
                    GeminiRequest.createTextRequest(
                        prompt = "질문 생성",
                        thinkingLevel = "medium",
                    )
                val json = objectMapper.writeValueAsString(request)

                Then("올바른 JSON 형식으로 변환되어야 한다") {
                    json.contains("\"text\":\"질문 생성\"") shouldBe true
                    json.contains("\"thinkingLevel\":\"medium\"") shouldBe true
                    json.contains("\"responseMimeType\":\"application/json\"") shouldBe true
                }
            }
        }

        Given("GeminiResponse를 파싱할 때") {

            When("정상적인 응답을 파싱하면") {
                val jsonResponse =
                    """
                    {
                        "candidates": [
                            {
                                "content": {
                                    "parts": [
                                        {"text": "[\"질문1\", \"질문2\", \"질문3\"]"}
                                    ],
                                    "role": "model"
                                },
                                "finishReason": "STOP",
                                "index": 0
                            }
                        ],
                        "usageMetadata": {
                            "promptTokenCount": 100,
                            "candidatesTokenCount": 50,
                            "totalTokenCount": 150
                        },
                        "modelVersion": "gemini-2.0-flash"
                    }
                    """.trimIndent()

                val response = objectMapper.readValue(jsonResponse, GeminiResponse::class.java)

                Then("candidates가 올바르게 파싱되어야 한다") {
                    response.candidates shouldNotBe null
                    response.candidates?.size shouldBe 1
                }

                Then("텍스트를 추출할 수 있어야 한다") {
                    val text = response.extractText()
                    text shouldBe "[\"질문1\", \"질문2\", \"질문3\"]"
                }

                Then("usageMetadata가 올바르게 파싱되어야 한다") {
                    response.usageMetadata?.promptTokenCount shouldBe 100
                    response.usageMetadata?.candidatesTokenCount shouldBe 50
                    response.usageMetadata?.totalTokenCount shouldBe 150
                }
            }

            When("thinking이 포함된 응답을 파싱하면") {
                val jsonResponse =
                    """
                    {
                        "candidates": [
                            {
                                "content": {
                                    "parts": [
                                        {"text": "생각 중...", "thought": true},
                                        {"text": "[\"실제 답변\"]", "thought": false}
                                    ],
                                    "role": "model"
                                }
                            }
                        ]
                    }
                    """.trimIndent()

                val response = objectMapper.readValue(jsonResponse, GeminiResponse::class.java)

                Then("thinking을 제외한 텍스트만 추출되어야 한다") {
                    val text = response.extractText()
                    text shouldBe "[\"실제 답변\"]"
                }
            }

            When("빈 candidates로 응답이 오면") {
                val jsonResponse =
                    """
                    {
                        "candidates": []
                    }
                    """.trimIndent()

                val response = objectMapper.readValue(jsonResponse, GeminiResponse::class.java)

                Then("extractText가 null을 반환해야 한다") {
                    response.extractText() shouldBe null
                }
            }

            When("에러 응답을 파싱하면") {
                val jsonResponse =
                    """
                    {
                        "candidates": null,
                        "error": {
                            "code": 400,
                            "message": "Invalid API key",
                            "status": "INVALID_ARGUMENT"
                        }
                    }
                    """.trimIndent()

                val response = objectMapper.readValue(jsonResponse, GeminiResponse::class.java)

                Then("error가 올바르게 파싱되어야 한다") {
                    response.error shouldNotBe null
                    response.error?.code shouldBe 400
                    response.error?.message shouldBe "Invalid API key"
                    response.error?.status shouldBe "INVALID_ARGUMENT"
                }
            }
        }
    })
