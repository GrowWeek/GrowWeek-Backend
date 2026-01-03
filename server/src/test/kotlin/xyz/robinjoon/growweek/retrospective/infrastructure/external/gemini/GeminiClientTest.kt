package xyz.robinjoon.growweek.retrospective.infrastructure.external.gemini

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class GeminiClientTest :
    BehaviorSpec({

        Given("GeminiProperties가 올바르게 설정되었을 때") {

            When("Properties를 생성하면") {
                val properties =
                    GeminiProperties(
                        apiKey = "test-api-key",
                        baseUrl = "https://generativelanguage.googleapis.com",
                        model = "gemini-3-flash-preview",
                        thinkingLevel = "low",
                    )

                Then("모든 값이 올바르게 설정되어야 한다") {
                    properties.apiKey shouldBe "test-api-key"
                    properties.baseUrl shouldBe "https://generativelanguage.googleapis.com"
                    properties.model shouldBe "gemini-3-flash-preview"
                    properties.thinkingLevel shouldBe "low"
                }
            }

            When("기본값을 사용하면") {
                val properties =
                    GeminiProperties(
                        apiKey = "test-api-key",
                    )

                Then("기본값이 적용되어야 한다") {
                    properties.baseUrl shouldBe "https://generativelanguage.googleapis.com"
                    properties.model shouldBe "gemini-2.0-flash"
                    properties.thinkingLevel shouldBe "low"
                }
            }
        }

        Given("GeminiApiException이 발생했을 때") {

            When("에러 코드와 메시지가 있으면") {
                val exception =
                    GeminiApiException(
                        message = "API 키가 유효하지 않습니다",
                        errorCode = 401,
                    )

                Then("메시지와 코드가 올바르게 설정되어야 한다") {
                    exception.message shouldBe "API 키가 유효하지 않습니다"
                    exception.errorCode shouldBe 401
                }
            }

            When("원인 예외가 있으면") {
                val cause = RuntimeException("원인")
                val exception =
                    GeminiApiException(
                        message = "API 호출 실패",
                        cause = cause,
                    )

                Then("원인이 올바르게 설정되어야 한다") {
                    exception.cause shouldBe cause
                }
            }
        }

        Given("GeminiRequest를 생성할 때") {

            When("createTextRequest로 요청을 생성하면") {
                val request =
                    GeminiRequest.createTextRequest(
                        prompt = "테스트 프롬프트입니다",
                        thinkingLevel = "medium",
                    )

                Then("contents에 프롬프트가 포함되어야 한다") {
                    request.contents.size shouldBe 1
                    request.contents[0].parts[0].text shouldBe "테스트 프롬프트입니다"
                }

                Then("generationConfig에 thinkingLevel이 설정되어야 한다") {
                    request.generationConfig?.thinkingConfig?.thinkingLevel shouldBe "medium"
                }

                Then("JSON 스키마가 배열 형식으로 설정되어야 한다") {
                    request.generationConfig?.responseSchema?.type shouldBe "array"
                    request.generationConfig
                        ?.responseSchema
                        ?.items
                        ?.type shouldBe "string"
                }
            }
        }

        Given("GeminiResponse에서 텍스트를 추출할 때") {

            When("정상적인 응답이면") {
                val response =
                    GeminiResponse(
                        candidates =
                            listOf(
                                GeminiResponse.Candidate(
                                    content =
                                        GeminiResponse.Content(
                                            parts =
                                                listOf(
                                                    GeminiResponse.Part(text = "답변 내용입니다"),
                                                ),
                                            role = "model",
                                        ),
                                    finishReason = "STOP",
                                ),
                            ),
                    )

                Then("텍스트가 추출되어야 한다") {
                    response.extractText() shouldBe "답변 내용입니다"
                }
            }

            When("여러 part가 있으면") {
                val response =
                    GeminiResponse(
                        candidates =
                            listOf(
                                GeminiResponse.Candidate(
                                    content =
                                        GeminiResponse.Content(
                                            parts =
                                                listOf(
                                                    GeminiResponse.Part(text = "첫 번째 "),
                                                    GeminiResponse.Part(text = "두 번째"),
                                                ),
                                            role = "model",
                                        ),
                                ),
                            ),
                    )

                Then("모든 텍스트가 합쳐져야 한다") {
                    response.extractText() shouldBe "첫 번째 두 번째"
                }
            }

            When("thought가 true인 part가 포함되어 있으면") {
                val response =
                    GeminiResponse(
                        candidates =
                            listOf(
                                GeminiResponse.Candidate(
                                    content =
                                        GeminiResponse.Content(
                                            parts =
                                                listOf(
                                                    GeminiResponse.Part(text = "생각...", thought = true),
                                                    GeminiResponse.Part(text = "실제 답변", thought = false),
                                                ),
                                            role = "model",
                                        ),
                                ),
                            ),
                    )

                Then("thought를 제외한 텍스트만 추출되어야 한다") {
                    response.extractText() shouldBe "실제 답변"
                }
            }

            When("candidates가 null이면") {
                val response = GeminiResponse(candidates = null)

                Then("null이 반환되어야 한다") {
                    response.extractText() shouldBe null
                }
            }

            When("candidates가 비어있으면") {
                val response = GeminiResponse(candidates = emptyList())

                Then("null이 반환되어야 한다") {
                    response.extractText() shouldBe null
                }
            }

            When("content가 null이면") {
                val response =
                    GeminiResponse(
                        candidates =
                            listOf(
                                GeminiResponse.Candidate(content = null),
                            ),
                    )

                Then("null이 반환되어야 한다") {
                    response.extractText() shouldBe null
                }
            }

            When("parts가 null이면") {
                val response =
                    GeminiResponse(
                        candidates =
                            listOf(
                                GeminiResponse.Candidate(
                                    content = GeminiResponse.Content(parts = null),
                                ),
                            ),
                    )

                Then("null이 반환되어야 한다") {
                    response.extractText() shouldBe null
                }
            }
        }
    })
