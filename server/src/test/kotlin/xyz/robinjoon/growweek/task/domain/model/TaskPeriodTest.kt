package xyz.robinjoon.growweek.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class TaskPeriodTest :
    BehaviorSpec({

        Given("할일 기간을 생성할 때") {
            val today = LocalDate.of(2025, 1, 6)

            When("시작일과 마감일이 같은 경우") {
                val period = TaskPeriod(today, today)

                Then("기간이 정상적으로 생성되어야 한다") {
                    period.startDate shouldBe today
                    period.dueDate shouldBe today
                }
            }

            When("마감일이 시작일보다 이후인 경우") {
                val period = TaskPeriod(today, today.plusDays(7))

                Then("기간이 정상적으로 생성되어야 한다") {
                    period.startDate shouldBe today
                    period.dueDate shouldBe today.plusDays(7)
                }
            }

            When("마감일이 시작일보다 이전인 경우") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        TaskPeriod(today, today.minusDays(1))
                    }.message shouldBe "마감일은 시작일보다 이전일 수 없습니다"
                }
            }
        }

        Given("특정 주와 할일 기간이 겹치는지 확인할 때") {
            // 주의 시작: 월요일 2025-01-06, 종료: 일요일 2025-01-12
            val weekStart = LocalDate.of(2025, 1, 6)
            val weekEnd = LocalDate.of(2025, 1, 12)

            When("할일 기간이 해당 주에 완전히 포함되는 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 7),
                        LocalDate.of(2025, 1, 10),
                    )

                Then("겹친다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe true
                }
            }

            When("할일 시작일이 해당 주 이전이고 마감일이 해당 주 내에 있는 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 8),
                    )

                Then("겹친다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe true
                }
            }

            When("할일 시작일이 해당 주 내에 있고 마감일이 해당 주 이후인 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 20),
                    )

                Then("겹친다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe true
                }
            }

            When("할일 기간이 해당 주를 완전히 포함하는 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 20),
                    )

                Then("겹친다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe true
                }
            }

            When("할일 마감일이 해당 주 시작일과 같은 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 6),
                    )

                Then("겹친다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe true
                }
            }

            When("할일 시작일이 해당 주 종료일과 같은 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 12),
                        LocalDate.of(2025, 1, 20),
                    )

                Then("겹친다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe true
                }
            }

            When("할일 기간이 해당 주 이전에 끝나는 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 5),
                    )

                Then("겹치지 않는다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe false
                }
            }

            When("할일 기간이 해당 주 이후에 시작하는 경우") {
                val period =
                    TaskPeriod(
                        LocalDate.of(2025, 1, 13),
                        LocalDate.of(2025, 1, 20),
                    )

                Then("겹치지 않는다고 판단해야 한다") {
                    period.overlaps(weekStart, weekEnd) shouldBe false
                }
            }
        }
    })
