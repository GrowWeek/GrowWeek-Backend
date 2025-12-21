package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.engine.concurrency.SpecExecutionMode
import io.kotest.engine.concurrency.TestExecutionMode
import io.kotest.extensions.spring.SpringExtension

class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf(SpringExtension())

    override val isolationMode: IsolationMode = IsolationMode.InstancePerRoot

    override val specExecutionMode: SpecExecutionMode = SpecExecutionMode.Concurrent

    override val testExecutionMode: TestExecutionMode = TestExecutionMode.Concurrent
}
