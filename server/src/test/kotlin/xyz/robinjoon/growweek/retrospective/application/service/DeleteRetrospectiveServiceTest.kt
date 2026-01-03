package xyz.robinjoon.growweek.retrospective.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository

class DeleteRetrospectiveServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val retrospectiveRepository = mockk<RetrospectiveRepository>()
        val service = DeleteRetrospectiveService(retrospectiveRepository)

        Given("회고 삭제 요청이 왔을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)

            val command =
                RetrospectiveApplicationCommand.DeleteRetrospective(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                )

            val commandSlot = slot<List<RetrospectiveCommand>>()
            every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns emptyList()

            When("서비스를 실행하면") {
                service.execute(command)

                Then("Repository에 삭제 요청을 해야 한다") {
                    verify(exactly = 1) { retrospectiveRepository.saveAll(any()) }
                }

                Then("Application Command가 Domain Command로 변환되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.DeleteRetrospective
                    capturedCommand.retrospectiveId shouldBe retrospectiveId
                    capturedCommand.memberId shouldBe memberId
                }
            }
        }
    })
