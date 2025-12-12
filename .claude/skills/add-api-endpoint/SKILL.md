---
name: add-api-endpoint
description: REST API 엔드포인트를 Presentation Layer에 추가하고 OpenAPI 문서를 생성합니다. API를 추가하거나 Controller, Request/Response DTO가 필요할 때 사용하세요.
---

# Add API Endpoint

## Instructions

### 1. Controller 작성

**위치**: `{bounded-context}/presentation/`

**애노테이션**:
- `@RestController`: REST API Controller 선언
- `@RequestMapping("/api/v1/{resource}")`: 기본 경로 설정
- HTTP 메서드: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`

### 2. Request DTO

**위치**: `{bounded-context}/presentation/request/`

**특징**:
- 유효성 검증: `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Email` 등
- 도메인 객체로 변환하는 `toCommand()` 또는 `toDomain()` 메서드 제공
- 불변 data class로 작성

```kotlin
data class CreateTaskRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    val description: String
) {
    fun toCommand() = CreateTaskCommand(
        title = title,
        description = description
    )
}
```

### 3. Response DTO

**위치**: `{bounded-context}/presentation/response/`

**특징**:
- 도메인 객체에서 변환하는 `from()` factory 메서드
- 필요한 정보만 노출 (민감 정보 제외)
- 불변 data class로 작성

```kotlin
data class TaskResponse(
    val id: String,
    val title: String,
    val description: String,
    val status: String
) {
    companion object {
        fun from(task: Task) = TaskResponse(
            id = task.id.value.toString(),
            title = task.title,
            description = task.description,
            status = task.status.name
        )
    }
}
```

### 4. OpenAPI 문서화

**애노테이션**:
- `@Tag`: Controller 그룹화
- `@Operation`: API 설명
- `@ApiResponse`: 응답 정의
- `@Parameter`: 파라미터 설명
- `@Schema`: DTO 스키마 설명

### 5. RESTful API 규칙

- **복수형 명사** 사용
- **소문자**, 하이픈 구분
- **동사 사용 지양**

**경로 예시**:
- `GET /api/v1/tasks`: 목록 조회
- `GET /api/v1/tasks/{id}`: 단건 조회
- `POST /api/v1/tasks`: 생성
- `PUT /api/v1/tasks/{id}`: 전체 수정
- `PATCH /api/v1/tasks/{id}`: 부분 수정
- `DELETE /api/v1/tasks/{id}`: 삭제

### 6. HTTP Status Code

- `200 OK`: 성공
- `201 Created`: 생성 성공
- `204 No Content`: 성공 (응답 본문 없음)
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 에러

## Examples

### 완전한 Controller 예시

```kotlin
// presentation/TaskController.kt
package xyz.robinjoon.growweek.task.presentation

@Tag(name = "Task", description = "작업 관리 API")
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(
    private val taskCommandService: TaskCommandService,
    private val taskQueryService: TaskQueryService
) {

    @Operation(summary = "작업 생성", description = "새로운 작업을 생성합니다")
    @ApiResponse(responseCode = "201", description = "작업 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @PostMapping
    fun createTask(
        @Valid @RequestBody request: CreateTaskRequest
    ): ResponseEntity<TaskResponse> {
        val taskId = taskCommandService.createTask(request.toCommand())
        val task = taskQueryService.findTaskById(taskId)
            ?: throw IllegalStateException("Task not found after creation")

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(TaskResponse.from(task))
    }

    @Operation(summary = "작업 조회", description = "ID로 작업을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "작업을 찾을 수 없음")
    @GetMapping("/{id}")
    fun getTask(
        @Parameter(description = "작업 ID", required = true)
        @PathVariable id: Long
    ): ResponseEntity<TaskResponse> {
        val task = taskQueryService.findTaskById(TaskId(id))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(TaskResponse.from(task))
    }

    @Operation(summary = "작업 목록 조회", description = "작업 목록을 검색합니다")
    @GetMapping
    fun searchTasks(
        @Parameter(description = "작업 상태")
        @RequestParam(required = false) status: TaskStatus?,

        @Parameter(description = "담당자 ID")
        @RequestParam(required = false) assigneeId: String?,

        @Parameter(description = "페이지 번호")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<TaskSummaryResponse>> {
        val query = SearchTasksQuery(status, assigneeId, page, size)
        val tasks = taskQueryService.searchTasks(query)

        return ResponseEntity.ok(tasks.map { TaskSummaryResponse.from(it) })
    }

    @Operation(summary = "작업 수정", description = "작업 정보를 수정합니다")
    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<TaskResponse> {
        val task = taskCommandService.updateTask(
            TaskId(id),
            request.toCommand()
        )

        return ResponseEntity.ok(TaskResponse.from(task))
    }

    @Operation(summary = "작업 삭제", description = "작업을 삭제합니다")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: Long): ResponseEntity<Void> {
        taskCommandService.deleteTask(TaskId(id))
        return ResponseEntity.noContent().build()
    }
}
```

### Request DTO with Validation

```kotlin
// presentation/request/CreateTaskRequest.kt
package xyz.robinjoon.growweek.task.presentation.request

@Schema(description = "작업 생성 요청")
data class CreateTaskRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
    @Schema(description = "작업 제목", example = "API 개발", required = true)
    val title: String,

    @field:NotBlank(message = "설명은 필수입니다")
    @Schema(description = "작업 설명", example = "사용자 관리 API 개발", required = true)
    val description: String,

    @Schema(description = "담당자 ID", example = "123")
    val assigneeId: String? = null
) {
    fun toCommand() = CreateTaskCommand(
        title = title.trim(),
        description = description.trim(),
        assigneeId = assigneeId
    )
}
```

### Response DTO

```kotlin
// presentation/response/TaskResponse.kt
package xyz.robinjoon.growweek.task.presentation.response

@Schema(description = "작업 응답")
data class TaskResponse(
    @Schema(description = "작업 ID", example = "1")
    val id: String,

    @Schema(description = "작업 제목", example = "API 개발")
    val title: String,

    @Schema(description = "작업 설명")
    val description: String,

    @Schema(description = "작업 상태", example = "PENDING")
    val status: String,

    @Schema(description = "생성 일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(task: Task) = TaskResponse(
            id = task.id.value.toString(),
            title = task.title,
            description = task.description,
            status = task.status.name,
            createdAt = task.createdAt
        )
    }
}
```
