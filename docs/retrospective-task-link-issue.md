# Task의 hasRetrospective가 false로 표시되는 문제 분석

## 문제 현상

회고(Retrospective)를 작성해도 연결된 할일(Task)의 API 응답에서 `hasRetrospective: false`로 반환됨.

```json
{
    "id": 1,
    "title": "할일",
    "status": "DONE",
    "hasRetrospective": false,  // 회고가 있어도 false
    ...
}
```

---

## 원인 분석

### 1. 데이터 모델 구조

**Task 테이블** (`TaskTable.kt`)
```kotlin
val retrospectiveId = long("retrospective_id").nullable()
```

**Task 도메인 모델** (`Task.kt`)
```kotlin
val retrospectiveId: RetrospectiveId? = null
```

Task는 `retrospectiveId` 필드를 통해 Retrospective와의 연결을 추적합니다.

### 2. hasRetrospective 계산 방식

**TaskDto.kt (라인 36)**
```kotlin
hasRetrospective = task.retrospectiveId != null
```

Task의 `retrospectiveId`가 null이 아닌지 확인하여 boolean 값으로 변환합니다.

### 3. 근본 원인: Task 연결 로직 누락

| 시점 | 수행 작업 | Task 연결 |
|------|----------|----------|
| 회고 생성 | Retrospective 테이블 저장 | **미수행** |
| 질문 생성 | Question 테이블 저장 | **미수행** |
| 답변 작성 | Answer 테이블 저장 | **미수행** |
| 회고 완료 | Retrospective 상태 변경 | **미수행** |

**Retrospective 생성/완료 시 Task 테이블의 `retrospective_id`를 업데이트하는 로직이 완전히 누락되었습니다.**

### 4. 구현 계획과의 차이

`retrospective-implementation-plan.md` 문서에서는 다음과 같이 명시:

```
4. QuestionGenerationService를 통해 AI 질문 생성
5. 회고에 질문 추가 및 상태 변경
6. Task에 RetrospectiveId 연결 (TaskRepository 사용)  <-- 미구현
```

6번 항목이 구현되지 않았습니다.

### 5. 미사용 코드 존재

**TaskCommand.kt (라인 57-60)**
```kotlin
data class LinkRetrospective(
    val taskId: TaskId,
    val retrospectiveId: RetrospectiveId
) : TaskCommand
```

`LinkRetrospective` 커맨드가 정의되어 있지만, Retrospective 모듈에서 이를 호출하지 않습니다.

---

## 해결 방안

### 방안 1: 회고 완료 시 Task 연결 (권장)

`CompleteRetrospectiveService`에서 회고 완료 시 해당 기간의 Task들을 연결

```kotlin
// CompleteRetrospectiveService.kt
override fun execute(command: RetrospectiveApplicationCommand.CompleteRetrospective): RetrospectiveDto {
    // 1. 회고 완료 처리
    val domainCommand = RetrospectiveCommand.CompleteRetrospective(...)
    val savedRetrospectives = retrospectiveRepository.saveAll(listOf(domainCommand))
    val completed = savedRetrospectives.first()

    // 2. 해당 기간의 Task 조회 및 연결
    val tasksInPeriod = taskRepository.findAll(
        TaskQuery.OffsetByUserIdAndPeriod(
            userId = completed.userId,
            startDate = completed.period.startDate,
            endDate = completed.period.endDate
        )
    ).items

    // 3. 각 Task에 회고 연결
    val linkCommands = tasksInPeriod.map { task ->
        TaskCommand.LinkRetrospective(task.id, completed.id)
    }
    taskRepository.saveAll(linkCommands)

    return RetrospectiveDto.from(completed)
}
```

### 방안 2: 질문 생성 시 Task 연결

`GenerateQuestionsService`에서 질문 생성 완료 시 Task 연결

### 고려사항

1. **트랜잭션 처리**: Retrospective 저장 + Task 연결이 원자적으로 처리되어야 함
2. **기존 데이터 마이그레이션**: 이미 생성된 회고에 대해 Task 연결 필요
3. **회고 삭제 시**: Task의 `retrospectiveId`를 null로 되돌려야 함

---

## 영향 범위

- `CompleteRetrospectiveService` 또는 `GenerateQuestionsService` 수정 필요
- `TaskRepository`에 기간 기반 조회 쿼리 추가 필요 (없는 경우)
- 기존 데이터 마이그레이션 스크립트 필요

---

## 결론

**문제 원인**: 회고 생성/완료 시 Task 테이블의 `retrospective_id` 필드를 업데이트하는 로직이 누락됨

**해결책**: 회고 완료 시점에 해당 기간의 Task들을 조회하여 `LinkRetrospective` 커맨드를 통해 연결 처리
