---
name: write-test
description: 테스트 코드를 작성할 때 이 정책을 따르세요. 단, 아키텍처 구조 테스트(ArchUnit)는 이 정책의 적용 대상이 아닙니다.
---

# 테스트 코드 작성 정책

이 문서는 GrowWeek 프로젝트의 테스트 코드 작성 가이드라인을 정의합니다.

## 예외사항

- **아키텍처 구조 테스트(ArchUnit)**: ArchUnit 기반의 패키지 의존성/계층 규칙 검증 테스트는 이 정책의 적용 대상이 아닙니다. 아키텍처 테스트는 BehaviorSpec/MockK이 아닌 ArchUnit + JUnit5 방식으로 작성되며, 별도의 컨벤션을 따릅니다.

## 1. 테스팅 프레임워크 및 도구

- **Test Framework**: [Kotest](https://kotest.io/) (Kotlin 전용 테스트 프레임워크)
- **Mocking**: [Mockk](https://mockk.io/) (Kotlin 전용 Mocking 라이브러리)
- **Assertion**: Kotest Assertions
- **Spring Integration**: Spring Boot Test
- **Spec Style**: `BehaviorSpec` (Given, When, Then 스타일)

## 2. 기본 원칙

1. **테스트 피라미드 준수**: Unit Test > Integration Test > E2E Test 비율을 지향합니다.
2. **가독성**: 테스트 코드는 그 자체로 문서의 역할을 해야 합니다. Kotest의 `Given`, `When`, `Then`을 명확히 사용하여 의도를 드러냅니다.
3. **격리**: 각 테스트는 독립적으로 실행되어야 하며, 실행 순서에 의존하지 않아야 합니다.
4. **네이밍**: 테스트 클래스명은 `대상클래스명` + `Test` 로 합니다. (예: `TaskServiceTest`)

## 3. 레이어별 테스트 전략

Clean Architecture 구조에 맞춰 각 레이어별 테스트 전략을 정의합니다.

### 3.1. Domain Layer (단위 테스트)

- **대상**: Entity, Value Object(VO), Domain Service
- **목표**: 비즈니스 로직의 정확성 검증
- **필수여부** : 필수
- **방식**:
    - 외부 의존성 없이 순수 Kotlin 코드로 작성합니다.
    - Spring Context를 띄우지 않습니다. (`POJO` 테스트)
    - 도메인 규칙, 불변식, 상태 변경 로직을 중점적으로 검증합니다.
- **예시**:
    ```kotlin
    class TaskTest : BehaviorSpec({
        Given("새로운 할 일을 생성할 때") {
            val title = TaskTitle("제목")
            val period = TaskPeriod(LocalDate.now(), LocalDate.now().plusDays(1))
            
            When("유효한 정보가 주어지면") {
                // val task = Task.create(...)
                
                Then("할 일이 생성되어야 한다") {
                    // task.title shouldBe title
                    // task.status shouldBe TaskStatus.TODO
                }
            }
            
            When("마감일이 시작일보다 빠르면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        TaskPeriod(LocalDate.now().plusDays(1), LocalDate.now())
                    }
                }
            }
        }
    })
    ```

### 3.2. Application Layer (단위 테스트)

- **대상**: Service (UseCase 구현체)
- **목표**: 애플리케이션 유스케이스 흐름 검증
- **필수여부** : 필수
- **방식**:
    - Repository, Domain Service 등 하위 레이어의 의존성은 `Mockk`를 사용하여 Mocking 합니다.
    - Spring Context를 띄우지 않습니다. (`POJO` 테스트)
    - 입력(Command)에 따른 도메인 로직 호출 및 결과(DTO) 반환을 검증합니다.
- **예시**:
    ```kotlin
    class CreateTaskServiceTest : BehaviorSpec({
        val taskRepository = mockk<TaskRepository>()
        val service = CreateTaskService(taskRepository)
        
        Given("할 일 생성 요청이 왔을 때") {
            val command = TaskApplicationCommand.CreateTask(...)
            
            // Mocking
            every { taskRepository.saveAll(any()) } returns listOf(mockTask)
            
            When("서비스를 실행하면") {
                val result = service.execute(command)
                
                Then("Repository에 저장하고 DTO를 반환해야 한다") {
                    verify(exactly = 1) { taskRepository.saveAll(any()) }
                    result.title.value shouldBe command.title
                }
            }
        }
    })
    ```

### 3.3. Infrastructure Layer (통합 테스트)

- **대상**: Repository 구현체 (Exposed), 외부 API Client
- **목표**: 실제 인프라와의 연동성 및 매핑 검증
- **필수여부** : 선택적 (중요한 부분에 한해 작성, 작성 여부 결정은 오직 사람이 판단)
- **방식**:
    - **Repository**:
        - H2 Database 등을 사용하여 실제 DB 환경과 유사하게 테스트합니다.
        - Spring Boot Test (`@SpringBootTest` 등)를 활용하여 필요한 Bean만 로드하거나 전체 Context를 로드합니다.
        - Exposed의 Query DSL이 올바르게 SQL로 변환되고 데이터가 저장/조회되는지 확인합니다.
- **예시**:
    ```kotlin
    @SpringBootTest
    class ExposedTaskRepositoryTest(
        private val taskRepository: TaskRepository
    ) : BehaviorSpec({
        extensions(SpringExtension) // Kotest Spring Extension 사용
        
        Given("TaskRepository가 주어졌을 때") {
            When("할 일을 저장하면") {
                val savedTask = taskRepository.saveAll(listOf(newTask)).first()
                
                Then("ID가 발급되어야 한다") {
                    savedTask.id shouldNotBe null
                }
            }
        }
    })
    ```

### 3.4. Presentation Layer (통합/단위 테스트)

- **대상**: Controller
- **목표**: HTTP 인터페이스 검증
- **필수여부** : 선택적 (중요한 부분에 한해 작성, 작성 여부 결정은 오직 사람이 판단)
- **방식**:
    - `@WebMvcTest`를 사용하여 Controller 관련 Bean만 로드합니다.
    - Service(UseCase)는 `@MockkBean`으로 Mocking 합니다.
    - HTTP Status Code, Request/Response Body 포맷, 예외 처리를 검증합니다.
- **예시**:
    ```kotlin
    @WebMvcTest(TaskController::class)
    class TaskControllerTest(
        @MockkBean private val createTaskUseCase: CreateTaskUseCase,
        private val mockMvc: MockMvc
    ) : BehaviorSpec({
        extensions(SpringExtension)

        Given("할 일 생성 API 요청이 왔을 때") {
            val request = CreateTaskRequest(...)
            every { createTaskUseCase.execute(any()) } returns taskDto
            
            When("POST 요청을 보내면") {
                val result = mockMvc.perform(post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                
                Then("201 Created 응답이 와야 한다") {
                    result.andExpect(status().isCreated)
                }
            }
        }
    })
    ```

## 4. Kotest 설정

### 4.1. Isolation Mode
- 테스트 간의 독립성을 보장하기 위해 필요에 따라 `IsolationMode.InstancePerLeaf` 등을 설정할 수 있습니다.
- 기본적으로는 상태 공유를 피하고, 각 테스트 케이스(Given/When/Then 블록) 내에서 필요한 데이터를 생성하여 사용합니다.

### 4.2. Project Config
- 전역 설정을 위해 `AbstractProjectConfig`를 구현한 클래스를 사용할 수 있습니다. (예: 병렬 실행 설정, 전역 Extension 등록 등)

## 5. 실행 및 CI

- 모든 테스트는 `./gradlew test` 명령으로 실행 가능해야 합니다.
- CI 파이프라인에서 테스트가 실패하면 빌드가 실패해야 합니다.
