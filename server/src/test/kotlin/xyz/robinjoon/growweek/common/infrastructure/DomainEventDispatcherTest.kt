package xyz.robinjoon.growweek.common.infrastructure

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.*
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.event.DefaultDomainEvent
import xyz.robinjoon.growweek.common.event.DomainEventHandler
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import java.time.LocalDate

class DomainEventDispatcherTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    Given("여러 핸들러가 등록되어 있을 때") {
        val handler1 = mockk<DomainEventHandler<RetrospectiveEventPayload.Completed>>()
        val handler2 = mockk<DomainEventHandler<RetrospectiveEventPayload.Deleted>>()

        every { handler1.supports(RetrospectiveEventPayload.Completed::class) } returns true
        every { handler1.supports(RetrospectiveEventPayload.Deleted::class) } returns false
        every { handler1.handle(any()) } just Runs

        every { handler2.supports(RetrospectiveEventPayload.Completed::class) } returns false
        every { handler2.supports(RetrospectiveEventPayload.Deleted::class) } returns true
        every { handler2.handle(any()) } just Runs

        val dispatcher = DomainEventDispatcher(listOf(handler1, handler2))

        When("Completed 이벤트가 디스패치되면") {
            val completedPayload = RetrospectiveEventPayload.Completed(
                retrospectiveId = RetrospectiveId(1L),
                memberId = MemberId(1L),
                startDate = LocalDate.of(2025, 1, 6),
                endDate = LocalDate.of(2025, 1, 12)
            )
            val event = DefaultDomainEvent(payload = completedPayload)

            dispatcher.dispatch(event)

            Then("Completed를 지원하는 handler1만 호출되어야 한다") {
                verify(exactly = 1) { handler1.supports(RetrospectiveEventPayload.Completed::class) }
                verify(exactly = 1) { handler1.handle(any()) }
            }

            Then("Deleted를 지원하는 handler2는 호출되지 않아야 한다") {
                verify(exactly = 1) { handler2.supports(RetrospectiveEventPayload.Completed::class) }
                verify(exactly = 0) { handler2.handle(any()) }
            }
        }

        When("Deleted 이벤트가 디스패치되면") {
            val deletedPayload = RetrospectiveEventPayload.Deleted(
                retrospectiveId = RetrospectiveId(1L),
                memberId = MemberId(1L)
            )
            val event = DefaultDomainEvent(payload = deletedPayload)

            dispatcher.dispatch(event)

            Then("Deleted를 지원하는 handler2만 호출되어야 한다") {
                verify(exactly = 1) { handler2.supports(RetrospectiveEventPayload.Deleted::class) }
                verify(exactly = 1) { handler2.handle(any()) }
            }

            Then("Completed를 지원하는 handler1은 호출되지 않아야 한다") {
                verify(exactly = 1) { handler1.supports(RetrospectiveEventPayload.Deleted::class) }
                verify(exactly = 0) { handler1.handle(any()) }
            }
        }
    }

    Given("핸들러가 없을 때") {
        val dispatcher = DomainEventDispatcher(emptyList())

        When("이벤트가 디스패치되면") {
            val payload = RetrospectiveEventPayload.Completed(
                retrospectiveId = RetrospectiveId(1L),
                memberId = MemberId(1L),
                startDate = LocalDate.of(2025, 1, 6),
                endDate = LocalDate.of(2025, 1, 12)
            )
            val event = DefaultDomainEvent(payload = payload)

            Then("예외 없이 처리되어야 한다") {
                // Should not throw
                dispatcher.dispatch(event)
            }
        }
    }
})
