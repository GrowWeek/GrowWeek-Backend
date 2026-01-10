package xyz.robinjoon.growweek.common.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class WeekIdTest :
    BehaviorSpec({

        Given("WeekId를 문자열로 생성할 때") {

            When("올바른 형식이 주어지면") {
                val weekId = WeekId("2024-W03")

                Then("WeekId가 정상적으로 생성되어야 한다") {
                    weekId.value shouldBe "2024-W03"
                    weekId.year shouldBe 2024
                    weekId.weekNumber shouldBe 3
                }
            }

            When("잘못된 형식이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        WeekId("2024-03")
                    }
                }
            }

            When("W 없이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        WeekId("202403")
                    }
                }
            }

            When("54주차가 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        WeekId("2024-W54")
                    }
                }
            }

            When("0주차가 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        WeekId("2024-W00")
                    }
                }
            }
        }

        Given("WeekId.of(year, weekNumber)로 생성할 때") {

            When("유효한 year와 weekNumber가 주어지면") {
                val weekId = WeekId.of(2024, 3)

                Then("올바른 형식의 WeekId가 생성되어야 한다") {
                    weekId.value shouldBe "2024-W03"
                }
            }

            When("한 자리 weekNumber가 주어지면") {
                val weekId = WeekId.of(2024, 1)

                Then("0이 패딩되어야 한다") {
                    weekId.value shouldBe "2024-W01"
                }
            }

            When("두 자리 weekNumber가 주어지면") {
                val weekId = WeekId.of(2024, 52)

                Then("패딩 없이 생성되어야 한다") {
                    weekId.value shouldBe "2024-W52"
                }
            }

            When("0주차가 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        WeekId.of(2024, 0)
                    }
                }
            }

            When("54주차가 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        WeekId.of(2024, 54)
                    }
                }
            }
        }

        Given("WeekId.of(LocalDate)로 생성할 때") {

            When("2024년 1월 15일(월요일, 3주차)이 주어지면") {
                val weekId = WeekId.of(LocalDate.of(2024, 1, 15))

                Then("2024-W03이 생성되어야 한다") {
                    weekId.value shouldBe "2024-W03"
                }
            }

            When("2024년 1월 21일(일요일, 3주차)이 주어지면") {
                val weekId = WeekId.of(LocalDate.of(2024, 1, 21))

                Then("2024-W03이 생성되어야 한다") {
                    weekId.value shouldBe "2024-W03"
                }
            }

            When("2024년 1월 1일(월요일, 1주차)이 주어지면") {
                val weekId = WeekId.of(LocalDate.of(2024, 1, 1))

                Then("2024-W01이 생성되어야 한다") {
                    weekId.value shouldBe "2024-W01"
                }
            }

            When("2024년 12월 30일(월요일)이 주어지면") {
                val weekId = WeekId.of(LocalDate.of(2024, 12, 30))

                Then("2025-W01이 생성되어야 한다 (ISO 8601 기준)") {
                    weekId.value shouldBe "2025-W01"
                }
            }

            When("2023년 1월 1일(일요일)이 주어지면") {
                val weekId = WeekId.of(LocalDate.of(2023, 1, 1))

                Then("2022-W52가 생성되어야 한다 (ISO 8601 기준)") {
                    weekId.value shouldBe "2022-W52"
                }
            }
        }

        Given("WeekId의 시작일과 종료일을 계산할 때") {

            When("2024년 3주차인 경우") {
                val weekId = WeekId("2024-W03")

                Then("시작일은 2024-01-15 (월요일)이어야 한다") {
                    weekId.startDate shouldBe LocalDate.of(2024, 1, 15)
                }

                Then("종료일은 2024-01-21 (일요일)이어야 한다") {
                    weekId.endDate shouldBe LocalDate.of(2024, 1, 21)
                }
            }

            When("2024년 1주차인 경우") {
                val weekId = WeekId("2024-W01")

                Then("시작일은 2024-01-01 (월요일)이어야 한다") {
                    weekId.startDate shouldBe LocalDate.of(2024, 1, 1)
                }

                Then("종료일은 2024-01-07 (일요일)이어야 한다") {
                    weekId.endDate shouldBe LocalDate.of(2024, 1, 7)
                }
            }

            When("2025년 1주차인 경우 (2024년 말에 시작)") {
                val weekId = WeekId("2025-W01")

                Then("시작일은 2024-12-30 (월요일)이어야 한다") {
                    weekId.startDate shouldBe LocalDate.of(2024, 12, 30)
                }

                Then("종료일은 2025-01-05 (일요일)이어야 한다") {
                    weekId.endDate shouldBe LocalDate.of(2025, 1, 5)
                }
            }
        }

        Given("날짜가 WeekId에 포함되는지 확인할 때") {
            val weekId = WeekId("2024-W03")

            When("주 시작일 (2024-01-15)이 주어지면") {
                Then("포함되어야 한다") {
                    weekId.contains(LocalDate.of(2024, 1, 15)) shouldBe true
                }
            }

            When("주 종료일 (2024-01-21)이 주어지면") {
                Then("포함되어야 한다") {
                    weekId.contains(LocalDate.of(2024, 1, 21)) shouldBe true
                }
            }

            When("주 중간 날짜 (2024-01-17)가 주어지면") {
                Then("포함되어야 한다") {
                    weekId.contains(LocalDate.of(2024, 1, 17)) shouldBe true
                }
            }

            When("주 시작일 하루 전 (2024-01-14)이 주어지면") {
                Then("포함되지 않아야 한다") {
                    weekId.contains(LocalDate.of(2024, 1, 14)) shouldBe false
                }
            }

            When("주 종료일 하루 후 (2024-01-22)가 주어지면") {
                Then("포함되지 않아야 한다") {
                    weekId.contains(LocalDate.of(2024, 1, 22)) shouldBe false
                }
            }
        }
    })
