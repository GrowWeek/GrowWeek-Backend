package xyz.robinjoon.growweek.retrospective.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class RetrospectivePeriodTest : BehaviorSpec({

    Given("회고 기간을 생성할 때") {

        When("시작일과 종료일이 같은 경우") {
            val date = LocalDate.of(2025, 1, 6)
            val period = RetrospectivePeriod(date, date)

            Then("기간이 정상적으로 생성되어야 한다") {
                period.startDate shouldBe date
                period.endDate shouldBe date
            }
        }

        When("종료일이 시작일보다 이후인 경우") {
            val startDate = LocalDate.of(2025, 1, 6)
            val endDate = LocalDate.of(2025, 1, 12)
            val period = RetrospectivePeriod(startDate, endDate)

            Then("기간이 정상적으로 생성되어야 한다") {
                period.startDate shouldBe startDate
                period.endDate shouldBe endDate
            }
        }

        When("시작일이 종료일보다 이후인 경우") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    RetrospectivePeriod(
                        LocalDate.of(2025, 1, 12),
                        LocalDate.of(2025, 1, 6)
                    )
                }.message shouldBe "시작일은 종료일보다 이전이어야 합니다"
            }
        }
    }

    Given("회고 작성 가능 여부를 확인할 때") {

        When("종료일이 월요일이고 현재 날짜가 다음 주 월요일 이전인 경우") {
            // 종료일: 2025-01-06 (월요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 6)
            )
            val currentDate = LocalDate.of(2025, 1, 10) // 금요일

            Then("작성 가능해야 한다") {
                period.isWritable(currentDate) shouldBe true
            }
        }

        When("종료일이 월요일이고 현재 날짜가 다음 주 월요일인 경우") {
            // 종료일: 2025-01-06 (월요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 6)
            )
            val currentDate = LocalDate.of(2025, 1, 13) // 다음 주 월요일

            Then("작성 가능해야 한다 (월요일 0시까지)") {
                period.isWritable(currentDate) shouldBe true
            }
        }

        When("종료일이 월요일이고 현재 날짜가 다음 주 월요일 이후인 경우") {
            // 종료일: 2025-01-06 (월요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 6)
            )
            val currentDate = LocalDate.of(2025, 1, 14) // 화요일

            Then("작성 불가능해야 한다") {
                period.isWritable(currentDate) shouldBe false
            }
        }

        When("종료일이 일요일이고 현재 날짜가 다음 날(월요일) 이전인 경우") {
            // 종료일: 2025-01-12 (일요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 12)
            )
            val currentDate = LocalDate.of(2025, 1, 12) // 일요일

            Then("작성 가능해야 한다") {
                period.isWritable(currentDate) shouldBe true
            }
        }

        When("종료일이 일요일이고 현재 날짜가 다음 날(월요일)인 경우") {
            // 종료일: 2025-01-12 (일요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 12)
            )
            val currentDate = LocalDate.of(2025, 1, 13) // 월요일

            Then("작성 가능해야 한다 (월요일 0시까지)") {
                period.isWritable(currentDate) shouldBe true
            }
        }

        When("종료일이 일요일이고 현재 날짜가 다음 날(월요일) 이후인 경우") {
            // 종료일: 2025-01-12 (일요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 12)
            )
            val currentDate = LocalDate.of(2025, 1, 14) // 화요일

            Then("작성 불가능해야 한다") {
                period.isWritable(currentDate) shouldBe false
            }
        }

        When("종료일이 수요일이고 현재 날짜가 다음 주 월요일 이전인 경우") {
            // 종료일: 2025-01-08 (수요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 8)
            )
            val currentDate = LocalDate.of(2025, 1, 10) // 금요일

            Then("작성 가능해야 한다") {
                period.isWritable(currentDate) shouldBe true
            }
        }

        When("종료일이 수요일이고 현재 날짜가 다음 주 월요일인 경우") {
            // 종료일: 2025-01-08 (수요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 8)
            )
            val currentDate = LocalDate.of(2025, 1, 13) // 월요일

            Then("작성 가능해야 한다") {
                period.isWritable(currentDate) shouldBe true
            }
        }

        When("종료일이 수요일이고 현재 날짜가 다음 주 화요일인 경우") {
            // 종료일: 2025-01-08 (수요일)
            // 다음 주 월요일: 2025-01-13
            val period = RetrospectivePeriod(
                LocalDate.of(2025, 1, 6),
                LocalDate.of(2025, 1, 8)
            )
            val currentDate = LocalDate.of(2025, 1, 14) // 화요일

            Then("작성 불가능해야 한다") {
                period.isWritable(currentDate) shouldBe false
            }
        }
    }
})
