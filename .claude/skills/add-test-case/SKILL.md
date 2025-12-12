---
name: add-test-case
description: Kotest와 MockK를 사용하여 테스트 케이스를 작성합니다. 단위 테스트, 통합 테스트, API 테스트가 필요할 때 사용하세요. 목표 커버리지는 70%입니다.
---

# Add Test Case

## Instructions

### 1. 테스트 프레임워크

- **Kotest 6.0.0**: Kotlin 친화적 테스트 프레임워크
- **MockK 1.14.6**: Kotlin 전용 모킹 라이브러리
- **JaCoCo**: 코드 커버리지 측정 (최소 70%)

### 2. 테스트 유형

**단위 테스트 (Unit Test)**:
- 단일 클래스/메서드 테스트
- 외부 의존성 모킹
- 빠른 실행 속도
- 위치: `src/test/kotlin/{package}/`

**통합 테스트 (Integration Test)**:
- 여러 컴포넌트 통합
- 실제 데이터베이스 사용 (H2)
- `@SpringBootTest` 활용

**API 테스트**:
- REST API 엔드포인트
- `MockMvc` 활용
- 요청/응답 검증

### 3. Kotest Spec Styles

**BehaviorSpec (Given-When-Then)** - 권장:
```kotlin
class MyTest : BehaviorSpec({
    given("조건") {
        `when`("실행") {
            then("검증") {
                // assertion
            }
        }
    }
})
```

**FunSpec**:
```kotlin
class MyTest : FunSpec({
    test("테스트명") {
        // test code
    }
})
```

### 4. Given-When-Then 패턴

- **Given (준비)**: 테스트 데이터 준비, Mock 설정
- **When (실행)**: 테스트 대상 메서드 실행
- **Then (검증)**: 결과 검증, Mock 호출 검증

### 5. 코드 커버리지

- **목표**: 최소 70%
- **확인**: `./gradlew test jacocoTestReport`
- **보고서**: `build/reports/jacoco/test/html/index.html`

## Examples

### Domain Layer 단위 테스트

```kotlin
// domain/model/command/TaskTest.kt
package xyz.robinjoon.growweek.task.domain.model.command

class TaskTest : BehaviorSpec({

    given("유효한 제목과 설명이 주어졌을 때") {
        val title = "Test Task"
        val description = "Test Description"

        `when`("Task를 생성하면") {
            val task = Task.create(title, description)

            then("Task가 생성되어야 한다") {
                task.title shouldBe title
                task.description shouldBe description
                task.status shouldBe TaskStatus.PENDING
                task.id.value shouldBeGreaterThan 0
            }
        }
    }

    given("빈 제목이 주어졌을 때") {
        val title = ""
        val description = "Test Description"

        `when`("Task를 생성하면") {
            then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    Task.create(title, description)
                }
            }
        }
    }

    given("완료되지 않은 Task가 있을 때") {
        val task = Task.create("Test", "Description")

        `when`("complete()를 호출하면") {
            val completed = task.complete()

            then("상태가 COMPLETED로 변경되어야 한다") {
                completed.status shouldBe TaskStatus.COMPLETED
            }
        }
    }

    given("이미 완료된 Task가 있을 때") {
        val task = Task.create("Test", "Description").complete()

        `when`("complete()를 다시 호출하면") {
            then("IllegalArgumentException이 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    task.complete()
                }
            }
        }
    }
})
```

### Application Layer 단위 테스트 (MockK 사용)

```kotlin
// application/service/TaskCommandServiceTest.kt
package xyz.robinjoon.growweek.task.application.service

class TaskCommandServiceTest : BehaviorSpec({

    val taskRepository = mockk<TaskRepository>()
    val userRepository = mockk<UserRepository>()
    val taskCommandService = TaskCommandService(taskRepository, userRepository)

    afterEach {
        clearAllMocks()
    }

    given("유효한 CreateTaskCommand가 주어졌을 때") {
        val command = CreateTaskCommand(
            title = "Test Task",
            description = "Test Description",
            assigneeId = "123"
        )

        val user = mockk<User> {
            every { id } returns UserId(123)
        }

        val savedTask = Task.create(
            command.title,
            command.description
        ).copy(id = TaskId(1))

        every { userRepository.findById(UserId(123)) } returns user
        every { taskRepository.save(any()) } returns savedTask

        `when`("createTask를 호출하면") {
            val taskId = taskCommandService.createTask(command)

            then("Task가 생성되고 저장되어야 한다") {
                taskId shouldBe savedTask.id

                verify(exactly = 1) {
                    userRepository.findById(UserId(123))
                    taskRepository.save(any())
                }
            }
        }
    }

    given("존재하지 않는 담당자 ID가 주어졌을 때") {
        val command = CreateTaskCommand(
            title = "Test Task",
            description = "Test Description",
            assigneeId = "999"
        )

        every { userRepository.findById(UserId(999)) } returns null

        `when`("createTask를 호출하면") {
            then("UserNotFoundException이 발생해야 한다") {
                shouldThrow<UserNotFoundException> {
                    taskCommandService.createTask(command)
                }

                verify(exactly = 1) {
                    userRepository.findById(UserId(999))
                }

                verify(exactly = 0) {
                    taskRepository.save(any())
                }
            }
        }
    }
})
```

### Repository 통합 테스트

```kotlin
// infrastructure/persistence/TaskRepositoryImplTest.kt
package xyz.robinjoon.growweek.task.infrastructure.persistence

@SpringBootTest
@Transactional
class TaskRepositoryImplTest : BehaviorSpec() {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    init {
        given("저장할 Task가 있을 때") {
            val task = Task.create("Test Task", "Description")

            `when`("save를 호출하면") {
                val saved = taskRepository.save(task)

                then("Task가 저장되고 ID가 할당되어야 한다") {
                    saved.id.value shouldBeGreaterThan 0
                    saved.title shouldBe task.title
                }
            }
        }

        given("저장된 Task가 있을 때") {
            val task = taskRepository.save(
                Task.create("Test Task", "Description")
            )

            `when`("findById로 조회하면") {
                val found = taskRepository.findById(task.id)

                then("저장된 Task를 조회할 수 있어야 한다") {
                    found shouldNotBe null
                    found?.id shouldBe task.id
                    found?.title shouldBe task.title
                }
            }
        }

        given("저장된 여러 Task가 있을 때") {
            val task1 = taskRepository.save(
                Task.create("Task 1", "Description 1")
            )
            val task2 = taskRepository.save(
                Task.create("Task 2", "Description 2")
                    .copy(status = TaskStatus.COMPLETED)
            )

            `when`("상태로 검색하면") {
                val pending = taskRepository.findAll(TaskStatus.PENDING)
                val completed = taskRepository.findAll(TaskStatus.COMPLETED)

                then("상태에 맞는 Task만 조회되어야 한다") {
                    pending.size shouldBe 1
                    pending[0].id shouldBe task1.id

                    completed.size shouldBe 1
                    completed[0].id shouldBe task2.id
                }
            }
        }
    }
}
```

### Controller API 테스트 (MockMvc)

```kotlin
// presentation/TaskControllerTest.kt
package xyz.robinjoon.growweek.task.presentation

@WebMvcTest(TaskController::class)
class TaskControllerTest : BehaviorSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var taskCommandService: TaskCommandService

    @MockkBean
    private lateinit var taskQueryService: TaskQueryService

    private val objectMapper = ObjectMapper()

    init {
        given("유효한 CreateTaskRequest가 주어졌을 때") {
            val request = CreateTaskRequest(
                title = "Test Task",
                description = "Test Description"
            )

            val taskId = TaskId(1)
            val task = Task.create(request.title, request.description)
                .copy(id = taskId)

            every {
                taskCommandService.createTask(any())
            } returns taskId

            every {
                taskQueryService.findTaskById(taskId)
            } returns task

            `when`("POST /api/v1/tasks를 호출하면") {
                val result = mockMvc.perform(
                    post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )

                then("201 Created 응답을 받아야 한다") {
                    result.andExpect(status().isCreated)
                        .andExpect(jsonPath("$.id").value(taskId.value.toString()))
                        .andExpect(jsonPath("$.title").value(request.title))
                }
            }
        }

        given("잘못된 요청이 주어졌을 때") {
            val request = CreateTaskRequest(
                title = "",  // 빈 제목
                description = "Test Description"
            )

            `when`("POST /api/v1/tasks를 호출하면") {
                val result = mockMvc.perform(
                    post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )

                then("400 Bad Request 응답을 받아야 한다") {
                    result.andExpect(status().isBadRequest)
                }
            }
        }

        given("저장된 Task가 있을 때") {
            val taskId = TaskId(1)
            val task = Task.create("Test Task", "Description")
                .copy(id = taskId)

            every {
                taskQueryService.findTaskById(taskId)
            } returns task

            `when`("GET /api/v1/tasks/{id}를 호출하면") {
                val result = mockMvc.perform(
                    get("/api/v1/tasks/${taskId.value}")
                )

                then("200 OK와 Task 정보를 받아야 한다") {
                    result.andExpect(status().isOk)
                        .andExpect(jsonPath("$.id").value(taskId.value.toString()))
                        .andExpect(jsonPath("$.title").value(task.title))
                }
            }
        }
    }
}
```

### 코드 커버리지 확인

```bash
# 테스트 실행 및 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 커버리지 검증 (70% 미만이면 빌드 실패)
./gradlew jacocoTestCoverageVerification
```

### JaCoCo 설정 (build.gradle.kts)

```kotlin
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",
                    "**/config/**",
                    "**/*Application*"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}
```
