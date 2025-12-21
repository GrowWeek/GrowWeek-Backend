package xyz.robinjoon.growweek

import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ServerApplicationSpec : BehaviorSpec({
    Given("Spring Context") {
        When("Application starts") {
            Then("Context loads successfully") {
                // Spring Context loading is checked by @SpringBootTest automatically
            }
        }
    }
})
