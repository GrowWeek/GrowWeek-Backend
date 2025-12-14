---
name: add-or-update-presentation
description: Bounded Context에 새로운 presentation 레이어를 추가하거나 수정할 때 사용하세요.
---

# Add or Update Presentation

## Instructions

새로운 프레젠테이션 레이어를 추가하거나 기존 프레젠테이션 레이어를 수정할 때 다음 규칙을 따르세요:

### 1. presentation 디렉토리

```
└── presentation/
│   ├── rest/
│   │   ├── request/
│   │   └── response/
│   │   └── controller/
```

내부 코드에는 도메인 레이어의 VO(Value Object) 클래스를 직접 사용하지 않고, presentation 레이어에 맞는 DTO(Data Transfer Object) 클래스를 정의하여 사용합니다.
별도의 전용 VO 클래스는 사용하지 않고 Premitve Type (String, Int, Long 등) 또는 데이터 클래스 형태의 DTO를 사용합니다.

### 2. rest 디렉토리
rest 디렉토리에는 REST API 관련 클래스들이 위치합니다. request, response, controller 하위 디렉토리를 포함합니다.

- request 디렉토리: 클라이언트로부터 들어오는 요청 데이터를 표현하는 DTO 클래스가 위치합니다.
- response 디렉토리: 클라이언트에게 반환하는 응답 데이터를 표현하는 DTO 클래스가 위치합니다.
- controller 디렉토리: REST API 엔드포인트를 정의하는 컨트롤러 클래스가 위치합니다.

예를 들어:

```kotlin
// Request DTO
data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val step: String,
)
```

```kotlin
// Response DTO
data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val step: String,
)
```

```kotlin
// Controller
@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService
) {
    @PostMapping
    fun createTask(@RequestBody request: CreateTaskRequest): ResponseEntity<TaskResponse> {
        val task = taskService.createTask(request)
        val response = TaskResponse(
            id = task.id,
            title = task.title,
            description = task.description,
            step = task.step
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
```

### 3. 별도 디렉토리

필요에 따라 presentation 레이어 내에 추가적인 디렉토리를 생성할 수 있습니다. 예를 들어, GraphQL API를 위한 디렉토리나 WebSocket 관련 디렉토리를 추가할 수 있습니다.
각 디렉토리의 하위에 rest 디렉토리와 유사하게 request, response, controller 등의 하위 디렉토리를 포함할 수 있습니다.

