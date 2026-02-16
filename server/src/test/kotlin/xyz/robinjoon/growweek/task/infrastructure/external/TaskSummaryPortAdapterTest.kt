package xyz.robinjoon.growweek.task.infrastructure.external

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.TaskSummaryPayload
import xyz.robinjoon.growweek.common.domain.TaskSummaryStatus
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.task.domain.model.Priority
import xyz.robinjoon.growweek.task.domain.model.Task
import xyz.robinjoon.growweek.task.domain.model.TaskDescription
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import xyz.robinjoon.growweek.task.domain.model.TaskTitle
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import java.time.LocalDateTime

class TaskSummaryPortAdapterTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val taskRepository = mockk<TaskRepository>()
        val adapter = TaskSummaryPortAdapter(taskRepository)

        Given("주간 할일 요약 조회 요청이 들어오면") {
            val memberId = MemberId(1L)
            val weekId = WeekId.of(2025, 3)
            val payload = TaskSummaryPayload(memberId = memberId, weekId = weekId, size = 37)

            val tasks =
                listOf(
                    createTask(TaskId(1L), memberId, weekId, "할일 1", "설명 1", TaskStatus.TODO, SensitivityLevel.NONE),
                    createTask(
                        TaskId(2L),
                        memberId,
                        weekId,
                        "할일 2",
                        null,
                        TaskStatus.IN_PROGRESS,
                        SensitivityLevel.TITLE_ONLY,
                    ),
                    createTask(TaskId(3L), memberId, weekId, "할일 3", "설명 3", TaskStatus.DONE, SensitivityLevel.NEVER),
                    createTask(TaskId(4L), memberId, weekId, "할일 4", "설명 4", TaskStatus.CANCEL, SensitivityLevel.NONE),
                )

            val querySlot = slot<TaskQuery>()
            every { taskRepository.findAll(capture(querySlot)) } returns
                OffsetPage(
                    items = tasks,
                    page = 0,
                    size = payload.size,
                    totalPage = 1,
                )

            When("어댑터를 호출하면") {
                val result = adapter.getWeeklyTaskSummaries(payload)

                Then("TaskRepository를 member/week 조건으로 조회해야 한다") {
                    verify(exactly = 1) { taskRepository.findAll(any()) }

                    val capturedQuery = querySlot.captured as TaskQuery.OffsetByMemberIdAndWeek
                    capturedQuery.memberId shouldBe memberId
                    capturedQuery.weekId shouldBe weekId
                    capturedQuery.size shouldBe payload.size
                }

                Then("Task 도메인을 공유 요약 모델로 변환해야 한다") {
                    result shouldHaveSize 4

                    result[0].title shouldBe "할일 1"
                    result[0].description shouldBe "설명 1"
                    result[0].status shouldBe TaskSummaryStatus.TODO
                    result[0].sensitivityLevel shouldBe SensitivityLevel.NONE

                    result[1].description shouldBe null
                    result[1].status shouldBe TaskSummaryStatus.IN_PROGRESS

                    result[2].status shouldBe TaskSummaryStatus.DONE
                    result[2].sensitivityLevel shouldBe SensitivityLevel.NEVER

                    result[3].status shouldBe TaskSummaryStatus.CANCEL
                }
            }
        }

        Given("조회된 할일이 없으면") {
            val payload = TaskSummaryPayload(memberId = MemberId(1L), weekId = WeekId.of(2025, 3), size = 10)
            every { taskRepository.findAll(any()) } returns
                OffsetPage(
                    items = emptyList(),
                    page = 0,
                    size = payload.size,
                    totalPage = 0,
                )

            When("어댑터를 호출하면") {
                val result = adapter.getWeeklyTaskSummaries(payload)

                Then("빈 리스트를 반환해야 한다") {
                    result shouldBe emptyList()
                }
            }
        }
    })

private fun createTask(
    taskId: TaskId,
    memberId: MemberId,
    weekId: WeekId,
    title: String,
    description: String?,
    status: TaskStatus,
    sensitivityLevel: SensitivityLevel,
): Task {
    val now = LocalDateTime.now()
    return Task(
        id = taskId,
        memberId = memberId,
        title = TaskTitle(title),
        description = description?.let(::TaskDescription),
        status = status,
        sensitivityLevel = sensitivityLevel,
        priority = Priority(1),
        weekId = weekId,
        dueDate = weekId.endDate,
        createdAt = now,
        updatedAt = now,
    )
}
