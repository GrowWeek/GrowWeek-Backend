package xyz.robinjoon.growweek.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.MemberId
import java.time.LocalDate
import java.time.LocalDateTime

class TaskTest : BehaviorSpec({

    Given("할일 수정 가능 여부를 확인할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우") {
            val task = baseTask.copy(retrospectiveId = null)

            Then("수정 가능해야 한다") {
                task.canModify() shouldBe true
            }
        }

        When("회고가 연결되어 있고 마감일이 현재 시점 이후인 경우") {
            val futureDate = LocalDate.now().plusDays(7)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.now(), futureDate)
            )

            Then("제한적 수정이 가능해야 한다") {
                task.canModify() shouldBe true
            }
        }

        When("회고가 연결되어 있고 마감일이 현재 시점 이전인 경우") {
            val pastDate = LocalDate.now().minusDays(1)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(pastDate.minusDays(7), pastDate)
            )

            Then("수정 불가능해야 한다") {
                task.canModify() shouldBe false
            }
        }
    }

    Given("할일 제목을 수정할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우") {
            val task = baseTask.copy(retrospectiveId = null)
            val newTitle = TaskTitle("수정된 제목")

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updateTitle(newTitle, null)
                updatedTask.title shouldBe newTitle
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이후인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newTitle = TaskTitle("수정된 제목")

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updateTitle(newTitle, retrospectiveDate)
                updatedTask.title shouldBe newTitle
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이전인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 20)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newTitle = TaskTitle("수정된 제목")

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    task.updateTitle(newTitle, retrospectiveDate)
                }.message shouldBe "회고가 작성된 할일은 수정할 수 없습니다"
            }
        }
    }

    Given("할일 상세 설명을 수정할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우") {
            val task = baseTask.copy(retrospectiveId = null)
            val newDescription = TaskDescription("수정된 설명")

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updateDescription(newDescription, null)
                updatedTask.description shouldBe newDescription
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이후인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newDescription = TaskDescription("수정된 설명")

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updateDescription(newDescription, retrospectiveDate)
                updatedTask.description shouldBe newDescription
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이전인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 20)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newDescription = TaskDescription("수정된 설명")

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    task.updateDescription(newDescription, retrospectiveDate)
                }.message shouldBe "회고가 작성된 할일은 수정할 수 없습니다"
            }
        }
    }

    Given("할일 상태를 수정할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우 TODO에서 IN_PROGRESS로 변경하면") {
            val task = baseTask.copy(retrospectiveId = null, status = TaskStatus.TODO)

            Then("상태가 변경되어야 한다") {
                val updatedTask = task.updateStatus(TaskStatus.IN_PROGRESS, null)
                updatedTask.status shouldBe TaskStatus.IN_PROGRESS
            }
        }

        When("회고가 연결되지 않은 경우 IN_PROGRESS에서 DONE으로 변경하면") {
            val task = baseTask.copy(retrospectiveId = null, status = TaskStatus.IN_PROGRESS)

            Then("상태가 변경되어야 한다") {
                val updatedTask = task.updateStatus(TaskStatus.DONE, null)
                updatedTask.status shouldBe TaskStatus.DONE
            }
        }

        When("회고가 연결되지 않은 경우 DONE에서 TODO로 변경하면") {
            val task = baseTask.copy(retrospectiveId = null, status = TaskStatus.DONE)

            Then("상태가 변경되어야 한다 (단계별 이동 제약 없음)") {
                val updatedTask = task.updateStatus(TaskStatus.TODO, null)
                updatedTask.status shouldBe TaskStatus.TODO
            }
        }

        When("회고가 연결되지 않은 경우 TODO에서 CANCEL로 변경하면") {
            val task = baseTask.copy(retrospectiveId = null, status = TaskStatus.TODO)

            Then("상태가 변경되어야 한다") {
                val updatedTask = task.updateStatus(TaskStatus.CANCEL, null)
                updatedTask.status shouldBe TaskStatus.CANCEL
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이후인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15)),
                status = TaskStatus.TODO
            )

            Then("상태 변경이 가능해야 한다") {
                val updatedTask = task.updateStatus(TaskStatus.IN_PROGRESS, retrospectiveDate)
                updatedTask.status shouldBe TaskStatus.IN_PROGRESS
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이전인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 20)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15)),
                status = TaskStatus.TODO
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    task.updateStatus(TaskStatus.IN_PROGRESS, retrospectiveDate)
                }.message shouldBe "회고가 작성된 할일은 수정할 수 없습니다"
            }
        }
    }

    Given("할일 중요도를 수정할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우") {
            val task = baseTask.copy(retrospectiveId = null)
            val newPriority = Priority(5)

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updatePriority(newPriority, null)
                updatedTask.priority shouldBe newPriority
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이후인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newPriority = Priority(5)

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updatePriority(newPriority, retrospectiveDate)
                updatedTask.priority shouldBe newPriority
            }
        }
    }

    Given("할일 민감도를 수정할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우") {
            val task = baseTask.copy(retrospectiveId = null)

            Then("NONE에서 TITLE_ONLY로 변경 가능해야 한다") {
                val updatedTask = task.updateSensitivityLevel(SensitivityLevel.TITLE_ONLY, null)
                updatedTask.sensitivityLevel shouldBe SensitivityLevel.TITLE_ONLY
            }

            Then("NONE에서 NEVER로 변경 가능해야 한다") {
                val updatedTask = task.updateSensitivityLevel(SensitivityLevel.NEVER, null)
                updatedTask.sensitivityLevel shouldBe SensitivityLevel.NEVER
            }
        }

        When("회고가 연결되어 있고 마감일이 회고 시점 이후인 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )

            Then("수정이 가능해야 한다") {
                val updatedTask = task.updateSensitivityLevel(SensitivityLevel.NEVER, retrospectiveDate)
                updatedTask.sensitivityLevel shouldBe SensitivityLevel.NEVER
            }
        }
    }

    Given("할일 마감일을 수정할 때") {
        val baseTask = createTask()

        When("회고가 연결되지 않은 경우") {
            val task = baseTask.copy(
                retrospectiveId = null,
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newDueDate = LocalDate.of(2025, 1, 20)

            Then("마감일 수정이 가능해야 한다") {
                val updatedTask = task.updateDueDate(newDueDate, null)
                updatedTask.period.dueDate shouldBe newDueDate
            }
        }

        When("회고가 연결되어 있고 마감일을 회고 시점 이후로 수정하는 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newDueDate = LocalDate.of(2025, 1, 20)

            Then("마감일 수정이 가능해야 한다") {
                val updatedTask = task.updateDueDate(newDueDate, retrospectiveDate)
                updatedTask.period.dueDate shouldBe newDueDate
            }
        }

        When("회고가 연결되어 있고 마감일을 회고 시점 이전으로 수정하려는 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newDueDate = LocalDate.of(2025, 1, 8)

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    task.updateDueDate(newDueDate, retrospectiveDate)
                }.message shouldBe "회고 시점 이전으로 마감일을 수정할 수 없습니다"
            }
        }

        When("회고가 연결되어 있고 마감일을 회고 시점과 같은 날로 수정하려는 경우") {
            val retrospectiveDate = LocalDate.of(2025, 1, 10)
            val task = baseTask.copy(
                retrospectiveId = RetrospectiveId(1),
                period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 15))
            )
            val newDueDate = LocalDate.of(2025, 1, 10)

            Then("예외가 발생해야 한다 (회고 시점 이후여야 함)") {
                shouldThrow<IllegalArgumentException> {
                    task.updateDueDate(newDueDate, retrospectiveDate)
                }.message shouldBe "회고 시점 이전으로 마감일을 수정할 수 없습니다"
            }
        }
    }

    Given("할일에 회고를 연결할 때") {
        val baseTask = createTask().copy(retrospectiveId = null)

        When("회고 ID가 주어지면") {
            val retrospectiveId = RetrospectiveId(1)

            Then("회고가 연결되어야 한다") {
                val updatedTask = baseTask.linkRetrospective(retrospectiveId)
                updatedTask.retrospectiveId shouldBe retrospectiveId
            }
        }
    }

    Given("할일이 특정 주에 속하는지 확인할 때") {
        val weekStart = LocalDate.of(2025, 1, 6) // 월요일
        val weekEnd = LocalDate.of(2025, 1, 12) // 일요일

        When("할일 기간이 해당 주와 겹치는 경우") {
            val task = createTask().copy(
                period = TaskPeriod(LocalDate.of(2025, 1, 8), LocalDate.of(2025, 1, 10)),
                status = TaskStatus.TODO
            )

            Then("해당 주의 할일로 간주되어야 한다") {
                task.belongsToWeek(weekStart, weekEnd) shouldBe true
            }
        }

        When("할일 시작일이 이번 주이고 마감일이 다음 주인 경우") {
            val task = createTask().copy(
                period = TaskPeriod(LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20)),
                status = TaskStatus.IN_PROGRESS
            )

            Then("해당 주의 할일로 간주되어야 한다") {
                task.belongsToWeek(weekStart, weekEnd) shouldBe true
            }
        }

        When("할일 기간이 해당 주와 겹치지 않는 경우") {
            val task = createTask().copy(
                period = TaskPeriod(LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 20)),
                status = TaskStatus.TODO
            )

            Then("해당 주의 할일로 간주되지 않아야 한다") {
                task.belongsToWeek(weekStart, weekEnd) shouldBe false
            }
        }

        When("마감일 이전에 DONE 상태로 완료된 경우") {
            val dueDate = LocalDate.of(2025, 1, 15)
            val completedAt = LocalDateTime.of(2025, 1, 10, 12, 0)
            val task = createTask().copy(
                period = TaskPeriod(LocalDate.of(2025, 1, 6), dueDate),
                status = TaskStatus.DONE,
                updatedAt = completedAt
            )

            Then("해당 주의 할일로 간주되지 않아야 한다 (마감일 이전 완료)") {
                task.belongsToWeek(weekStart, weekEnd) shouldBe false
            }
        }

        When("마감일에 DONE 상태로 완료된 경우") {
            val dueDate = LocalDate.of(2025, 1, 10)
            val completedAt = LocalDateTime.of(2025, 1, 10, 12, 0)
            val task = createTask().copy(
                period = TaskPeriod(LocalDate.of(2025, 1, 6), dueDate),
                status = TaskStatus.DONE,
                updatedAt = completedAt
            )

            Then("해당 주의 할일로 간주되어야 한다") {
                task.belongsToWeek(weekStart, weekEnd) shouldBe true
            }
        }

        When("마감일 이후에 DONE 상태로 완료된 경우") {
            val dueDate = LocalDate.of(2025, 1, 10)
            val completedAt = LocalDateTime.of(2025, 1, 12, 12, 0)
            val task = createTask().copy(
                period = TaskPeriod(LocalDate.of(2025, 1, 6), dueDate),
                status = TaskStatus.DONE,
                updatedAt = completedAt
            )

            Then("해당 주의 할일로 간주되어야 한다") {
                task.belongsToWeek(weekStart, weekEnd) shouldBe true
            }
        }
    }
})

private fun createTask(): Task {
    val now = LocalDateTime.now()
    return Task(
        id = TaskId(1),
        memberId = MemberId(1L),
        title = TaskTitle("테스트 할일"),
        description = TaskDescription("테스트 설명"),
        status = TaskStatus.TODO,
        sensitivityLevel = SensitivityLevel.NONE,
        priority = Priority(1),
        period = TaskPeriod(LocalDate.now(), LocalDate.now().plusDays(7)),
        createdAt = now,
        updatedAt = now,
        retrospectiveId = null
    )
}
