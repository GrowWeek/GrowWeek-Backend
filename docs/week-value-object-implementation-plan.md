# WeekId 값 객체 구현 계획

## 개요

Common(Shared Kernel)에 WeekId 값 객체를 추가하여 기간 관련 복잡성을 해소합니다.

**선택한 옵션**: 옵션 1 - Common에 WeekId 값 객체만 추가

**설계 원칙**:
- WeekId는 순수한 식별자 + 날짜 계산만 담당
- 비즈니스 로직(isWritable 등)은 각 BC에서 구현

---

## 목표

1. `startDate`/`endDate` 직접 비교 → `WeekId` 비교로 단순화
2. 겹침 로직 4곳 → 1곳으로 통합
3. 테스트 케이스 30개+ → 대폭 감소
4. BC 간 기간 개념 통일
5. 비즈니스 로직은 해당 BC에서 관리

---

## Phase 1: WeekId 값 객체 추가 ✅ 완료

### 1.1 WeekId 값 객체

**위치**: `common/domain/WeekId.kt`

```kotlin
@JvmInline
value class WeekId(val value: String) {
    init {
        require(value.matches(WEEK_ID_PATTERN)) {
            "WeekId must be in format 'YYYY-Www' (e.g., '2024-W03'), but was '$value'"
        }
    }

    val year: Int get() = value.substringBefore("-W").toInt()
    val weekNumber: Int get() = value.substringAfter("-W").toInt()

    val startDate: LocalDate
        get() = LocalDate.of(year, 1, 4)
            .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNumber.toLong())
            .with(DayOfWeek.MONDAY)

    val endDate: LocalDate get() = startDate.plusDays(6)

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)

    companion object {
        private val WEEK_ID_PATTERN = Regex("""\d{4}-W(0[1-9]|[1-4]\d|5[0-3])""")

        fun of(year: Int, weekNumber: Int): WeekId
        fun of(date: LocalDate): WeekId
    }
}
```

**Week 클래스는 생성하지 않음** - 비즈니스 로직(isWritable 등)은 각 BC에서 구현

### 1.2 테스트

**위치**: `common/domain/WeekIdTest.kt`

- WeekId 생성 테스트 (문자열, year+weekNumber, LocalDate)
- startDate, endDate 계산 테스트
- contains() 테스트
- ISO 8601 연말 주차 처리 테스트

---

## Phase 2: Task BC 변경 ✅ 완료

### 2.1 Task 엔티티에 weekId 추가

**파일**: `task/domain/model/Task.kt`

```kotlin
// Before
data class Task(
    val id: TaskId,
    val memberId: MemberId,
    val period: TaskPeriod,  // startDate + dueDate
    val retrospectiveId: RetrospectiveId?,
    // ...
)

// After
data class Task(
    val id: TaskId,
    val memberId: MemberId,
    val weekId: WeekId,           // 새로 추가
    val dueDate: LocalDate,       // period에서 분리 (주 내 마감일)
    val retrospectiveId: RetrospectiveId?,
    // ...
) {
    // period.overlaps() 대신 weekId 비교
    fun belongsToWeek(targetWeekId: WeekId): Boolean = weekId == targetWeekId
}
```

### 2.2 TaskPeriod 제거

**삭제 파일**: `task/domain/model/TaskPeriod.kt`

**삭제 파일**: `task/domain/model/TaskPeriodTest.kt` (14개 테스트 제거)

### 2.3 CompletedRetrospectivePeriod → CompletedWeek 변경

**파일**: `task/domain/model/CompletedWeek.kt` (이름 변경)

```kotlin
// Before: CompletedRetrospectivePeriod
data class CompletedRetrospectivePeriod(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completedAt: LocalDateTime,
) {
    fun overlaps(periodStart: LocalDate, periodEnd: LocalDate): Boolean = ...
    fun contains(date: LocalDate): Boolean = ...
}

// After: CompletedWeek
data class CompletedWeek(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val weekId: WeekId,           // startDate + endDate 대신
    val completedAt: LocalDateTime,
)
```

### 2.4 CreateTaskService 검증 로직 단순화

**파일**: `task/application/service/CreateTaskService.kt`

```kotlin
// Before
private fun validateNotInCompletedRetrospectivePeriod(command: CreateTask) {
    val query = CompletedRetrospectivePeriodQuery.Offset.byMemberIdAndOverlappingPeriod(
        memberId = command.memberId,
        periodStart = command.startDate,
        periodEnd = command.dueDate,
    )
    val overlappingPeriods = completedRetrospectivePeriodRepository.findAll(query).items
    if (overlappingPeriods.isNotEmpty()) {
        throw IllegalArgumentException(
            "회고가 완료된 기간(${period.startDate} ~ ${period.endDate})에는 할일을 추가할 수 없습니다."
        )
    }
}

// After
private fun validateNotInCompletedWeek(command: CreateTask) {
    val weekId = WeekId.of(command.dueDate)
    val exists = completedWeekRepository.existsByMemberIdAndWeekId(
        memberId = command.memberId,
        weekId = weekId
    )
    if (exists) {
        throw IllegalArgumentException("회고가 완료된 주($weekId)에는 할일을 추가할 수 없습니다.")
    }
}
```

### 2.5 Task Repository 쿼리 변경

**파일**: `task/infrastructure/persistence/ExposedTaskRepository.kt`

```kotlin
// Before: 범위 겹침 쿼리
is TaskQuery.CursorByMemberIdAndWeek -> {
    baseQuery = baseQuery.andWhere {
        (TaskTable.userId eq query.memberId.value) and
        (TaskTable.startDate lessEq query.weekEnd) and
        (TaskTable.dueDate greaterEq query.weekStart)
    }
}

// After: WeekId 일치 쿼리
is TaskQuery.CursorByMemberIdAndWeek -> {
    baseQuery = baseQuery.andWhere {
        (TaskTable.userId eq query.memberId.value) and
        (TaskTable.weekId eq query.weekId.value)
    }
}
```

### 2.6 TaskTable 스키마 변경

**파일**: `task/infrastructure/persistence/TaskTable.kt`

```kotlin
// Before
object TaskTable : Table("tasks") {
    val startDate = date("start_date")
    val dueDate = date("due_date")
    // ...
}

// After
object TaskTable : Table("tasks") {
    val weekId = varchar("week_id", 8)   // "2024-W03" 형태
    val dueDate = date("due_date")       // 주 내 마감일 유지
    // startDate 컬럼 제거
}
```

---

## Phase 3: Retrospective BC 변경

### 3.1 Retrospective 엔티티에 weekId 추가

**파일**: `retrospective/domain/model/Retrospective.kt`

```kotlin
// Before
data class Retrospective(
    val id: RetrospectiveId,
    val memberId: MemberId,
    val period: RetrospectivePeriod,  // startDate + endDate
    // ...
)

// After
data class Retrospective(
    val id: RetrospectiveId,
    val memberId: MemberId,
    val weekId: WeekId,  // period 대신
    // ...
) {
    // 비즈니스 로직은 Retrospective BC 내에서 구현
    fun isWritable(currentDate: LocalDate = LocalDate.now()): Boolean {
        val writableStart = weekId.endDate.minusDays(2)  // 금요일
        val writableEnd = weekId.endDate.plusDays(1)     // 다음 월요일
        return !currentDate.isBefore(writableStart) && currentDate.isBefore(writableEnd)
    }
}
```

### 3.2 RetrospectivePeriod 제거

**삭제 파일**: `retrospective/domain/model/RetrospectivePeriod.kt`

**삭제 파일**: `retrospective/domain/model/RetrospectivePeriodTest.kt` (16개 테스트 제거)

- `isWritable()` 로직 → `Retrospective.isWritable()`로 이동 (BC 내에서 관리)
- `calculateNextMonday()` → 필요 없음 (endDate + 1일로 단순화)

### 3.3 RetrospectiveTable 스키마 변경

**파일**: `retrospective/infrastructure/persistence/RetrospectiveTable.kt`

```kotlin
// Before
object RetrospectiveTable : Table("retrospectives") {
    val startDate = date("start_date")
    val endDate = date("end_date")
    // ...
}

// After
object RetrospectiveTable : Table("retrospectives") {
    val weekId = varchar("week_id", 8)  // "2024-W03" 형태
    // startDate, endDate 컬럼 제거
}
```

### 3.4 이벤트 페이로드 변경

**파일**: `common/event/RetrospectiveEventPayload.kt`

```kotlin
// Before
data class Completed(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val startDate: LocalDate,
    val endDate: LocalDate,
) : RetrospectiveEventPayload

// After
data class Completed(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val weekId: WeekId,  // startDate + endDate 대신
) : RetrospectiveEventPayload
```

---

## Phase 4: DB 마이그레이션

### 4.1 마이그레이션 스크립트

```sql
-- 1. tasks 테이블에 week_id 컬럼 추가
ALTER TABLE tasks ADD COLUMN week_id VARCHAR(8);

-- 2. 기존 데이터 마이그레이션 (start_date 기준)
UPDATE tasks
SET week_id = CONCAT(
    EXTRACT(ISOYEAR FROM start_date),
    '-W',
    LPAD(EXTRACT(WEEK FROM start_date)::TEXT, 2, '0')
);

-- 3. NOT NULL 제약 추가
ALTER TABLE tasks ALTER COLUMN week_id SET NOT NULL;

-- 4. 인덱스 추가
CREATE INDEX idx_tasks_member_week ON tasks(user_id, week_id);

-- 5. start_date 컬럼 제거 (확인 후)
-- ALTER TABLE tasks DROP COLUMN start_date;

-- 6. retrospectives 테이블 동일하게 처리
ALTER TABLE retrospectives ADD COLUMN week_id VARCHAR(8);

UPDATE retrospectives
SET week_id = CONCAT(
    EXTRACT(ISOYEAR FROM start_date),
    '-W',
    LPAD(EXTRACT(WEEK FROM start_date)::TEXT, 2, '0')
);

ALTER TABLE retrospectives ALTER COLUMN week_id SET NOT NULL;
CREATE INDEX idx_retrospectives_member_week ON retrospectives(member_id, week_id);

-- 7. completed_retrospective_periods 테이블 변경
ALTER TABLE completed_retrospective_periods ADD COLUMN week_id VARCHAR(8);

UPDATE completed_retrospective_periods
SET week_id = CONCAT(
    EXTRACT(ISOYEAR FROM start_date),
    '-W',
    LPAD(EXTRACT(WEEK FROM start_date)::TEXT, 2, '0')
);

-- 8. 테이블명 변경 (선택)
-- ALTER TABLE completed_retrospective_periods RENAME TO completed_weeks;
```

---

## Phase 5: 제거 대상 코드

### 5.1 삭제할 파일

| 파일 | 이유 |
|------|------|
| `task/domain/model/TaskPeriod.kt` | WeekId로 대체 |
| `task/domain/model/TaskPeriodTest.kt` | 14개 테스트 불필요 |
| `retrospective/domain/model/RetrospectivePeriod.kt` | WeekId로 대체 |
| `retrospective/domain/model/RetrospectivePeriodTest.kt` | 16개 테스트 불필요 |

### 5.2 제거할 로직

| 위치 | 로직 |
|------|------|
| `TaskPeriod.overlaps()` | WeekId 비교로 대체 |
| `CompletedRetrospectivePeriod.overlaps()` | 불필요 |
| `CompletedRetrospectivePeriod.contains()` | 불필요 |
| `RetrospectivePeriod.isWritable()` | Retrospective.isWritable()로 이동 (BC 내) |
| `RetrospectivePeriod.calculateNextMonday()` | 불필요 (endDate + 1일로 단순화) |
| Repository SQL 범위 겹침 쿼리 | WeekId = 쿼리로 단순화 |

---

## 예상 효과

### 코드 복잡도 감소

| 지표 | Before | After |
|------|--------|-------|
| 겹침 로직 구현 | 4곳 | 0곳 |
| 기간 관련 테스트 | 30개+ | 10개 이하 |
| 날짜 비교 연산 | 20회+ | 5회 이하 |
| 시간축 개념 | 4가지 | 1가지 (WeekId) |

### 검증 로직 단순화

```kotlin
// Before: 복잡한 범위 겹침
!(dueDate.isBefore(weekStart) || startDate.isAfter(weekEnd))

// After: 단순 비교
task.weekId == targetWeekId
```

### 쿼리 성능 향상

```sql
-- Before: 범위 검색
WHERE start_date <= ? AND due_date >= ?

-- After: 등가 검색 (인덱스 효율적)
WHERE week_id = ?
```

---

## 구현 순서

1. **Phase 1**: WeekId 값 객체 추가 + 테스트 ✅ 완료
2. **Phase 4**: DB 마이그레이션 스크립트 작성 및 실행
3. **Phase 2**: Task BC 변경 ✅ 완료
4. **Phase 3**: Retrospective BC 변경 (isWritable 로직 BC 내에서 구현)
5. **Phase 5**: 레거시 코드 제거
6. **Phase 6**: API 엔드포인트 변경
7. 전체 테스트 실행 및 검증

---

## 주의사항

1. **연말 주차 처리**: ISO 8601 기준 12월 마지막 주가 다음 해 1주차가 될 수 있음
   - 예: 2024-12-30 → 2025-W01
   - `WeekFields.ISO` 사용으로 자동 처리됨

2. **기존 데이터 마이그레이션**: startDate 기준으로 weekId 계산
   - 마감일(dueDate)이 다른 주에 걸치는 경우는 startDate 주로 귀속

3. **API 호환성**: 기존 API 응답에 startDate, endDate 포함 시
   - Week에서 계산하여 반환하거나
   - API 버전업 고려

---

## Phase 6: API 엔드포인트 변경

### 6.1 현재 API에서 날짜 사용 현황

#### Task API

| 엔드포인트 | 요청 필드 | 응답 필드 |
|-----------|----------|----------|
| `POST /api/v1/tasks` | startDate, dueDate | startDate, dueDate |
| `PUT /api/v1/tasks/{id}` | dueDate (선택) | startDate, dueDate |
| `GET /api/v1/tasks/{id}` | - | startDate, dueDate |
| `GET /api/v1/tasks/weekly` | weekStart (쿼리) | weekStart, weekEnd, tasks |

#### Retrospective API

| 엔드포인트 | 요청 필드 | 응답 필드 |
|-----------|----------|----------|
| `POST /api/v1/retrospectives` | startDate, endDate | startDate, endDate |
| `GET /api/v1/retrospectives/{id}` | - | startDate, endDate |
| `GET /api/v1/retrospectives` | - | startDate, endDate |

---

### 6.2 API 변경 전략: 하위 호환성 유지

**원칙**: 기존 클라이언트가 깨지지 않도록 요청/응답 형식은 유지하되, 내부적으로 weekId 사용

#### Task API 변경

**CreateTaskRequest** - 변경 없음 (내부 처리만 변경)

```kotlin
// Before: 그대로 저장
data class CreateTaskRequest(
    val title: String,
    val startDate: String,   // 유지
    val dueDate: String,     // 유지
    // ...
)

// Controller에서 weekId 계산
fun createTask(request: CreateTaskRequest): TaskResponse {
    val weekId = WeekId.of(LocalDate.parse(request.startDate))
    val command = CreateTask(
        weekId = weekId,
        dueDate = LocalDate.parse(request.dueDate),
        // ...
    )
}
```

**TaskResponse** - startDate는 Week에서 계산

```kotlin
// Before
data class TaskResponse(
    val startDate: String,  // DB에서 직접
    val dueDate: String,
    // ...
)

// After
data class TaskResponse(
    val startDate: String,  // Week.startDate에서 계산
    val dueDate: String,    // Task.dueDate 유지
    val weekId: String?,    // 선택: 새 필드 추가 (nullable로 하위호환)
    // ...
)

// DTO 변환
fun TaskDto.toResponse(): TaskResponse {
    return TaskResponse(
        startDate = this.weekId.startDate.toString(),  // WeekId에서 계산
        dueDate = this.dueDate.toString(),
        weekId = this.weekId.value,  // 새 필드
        // ...
    )
}
```

**WeeklyTaskResponse** - weekStart → weekId 내부 변환

```kotlin
// 요청: 기존 방식 유지
GET /api/v1/tasks/weekly?weekStart=2024-01-15

// Controller
fun getWeeklyTasks(@RequestParam weekStart: String): WeeklyTaskResponse {
    val weekId = WeekId.of(LocalDate.parse(weekStart))  // 변환

    return WeeklyTaskResponse(
        weekStart = weekId.startDate.toString(),  // WeekId에서 계산
        weekEnd = weekId.endDate.toString(),      // WeekId에서 계산
        weekId = weekId.value,  // 새 필드 (선택)
        tasks = ...
    )
}
```

---

#### Retrospective API 변경

**CreateRetrospectiveRequest** - 변경 없음 (내부 처리만 변경)

```kotlin
// Before
data class CreateRetrospectiveRequest(
    val startDate: String,
    val endDate: String,
    // ...
)

// Controller에서 weekId 계산 + 검증
fun createRetrospective(request: CreateRetrospectiveRequest): RetrospectiveResponse {
    val startDate = LocalDate.parse(request.startDate)
    val endDate = LocalDate.parse(request.endDate)

    // 검증: startDate와 endDate가 같은 주인지
    val startWeekId = WeekId.of(startDate)
    val endWeekId = WeekId.of(endDate)
    require(startWeekId == endWeekId) {
        "startDate와 endDate는 같은 주에 속해야 합니다."
    }

    val command = CreateRetrospective(
        weekId = startWeekId,
        // ...
    )
}
```

**RetrospectiveResponse** - startDate, endDate는 Week에서 계산

```kotlin
data class RetrospectiveResponse(
    val startDate: String,  // Week.startDate
    val endDate: String,    // Week.endDate
    val weekId: String?,    // 새 필드 (선택)
    // ...
)

fun RetrospectiveDto.toResponse(): RetrospectiveResponse {
    return RetrospectiveResponse(
        startDate = this.weekId.startDate.toString(),  // WeekId에서 계산
        endDate = this.weekId.endDate.toString(),      // WeekId에서 계산
        weekId = this.weekId.value,
        // ...
    )
}
```

---

### 6.3 선택: API v2 도입

하위 호환성 대신 새 API 버전을 도입할 경우:

#### v2 Task API

```kotlin
// 요청: weekId 직접 사용
POST /api/v2/tasks
{
    "weekId": "2024-W03",     // 새로운 방식
    "dueDate": "2024-01-19",
    "title": "..."
}

// 또는 dueDate만으로 weekId 자동 계산
POST /api/v2/tasks
{
    "dueDate": "2024-01-19",  // weekId는 서버에서 계산
    "title": "..."
}

// 응답
{
    "id": 1,
    "weekId": "2024-W03",
    "startDate": "2024-01-15",  // Week에서 계산 (호환용)
    "endDate": "2024-01-21",    // Week에서 계산 (호환용)
    "dueDate": "2024-01-19",
    // ...
}
```

#### v2 주간 조회

```kotlin
// 기존: 날짜로 조회
GET /api/v1/tasks/weekly?weekStart=2024-01-15

// v2: weekId로 조회
GET /api/v2/tasks/weekly?weekId=2024-W03

// 또는 현재 주 조회
GET /api/v2/tasks/weekly/current
```

---

### 6.4 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `TaskController.kt` | weekStart → weekId 변환 로직 추가 |
| `RetrospectiveController.kt` | startDate/endDate → weekId 변환 로직 추가 |
| `CreateTaskRequest.kt` | 변경 없음 (하위호환) |
| `TaskResponse.kt` | weekId 필드 추가 (nullable) |
| `CreateRetrospectiveRequest.kt` | 변경 없음 (하위호환) |
| `RetrospectiveResponse.kt` | weekId 필드 추가 (nullable) |
| `WeeklyTaskResponse.kt` | weekId 필드 추가 (nullable) |

---

### 6.5 API 변경 요약

| 구분 | Before | After |
|------|--------|-------|
| **요청** | startDate, endDate 필수 | 유지 (내부에서 weekId 계산) |
| **응답** | startDate, endDate | 유지 + weekId 추가 (선택) |
| **주간 조회 파라미터** | weekStart | 유지 (내부에서 weekId 변환) |
| **내부 처리** | 날짜 범위 겹침 계산 | weekId 비교 |

**결론**: API 요청/응답 형식은 그대로 유지하고, Presentation → Application 레이어 경계에서 weekId로 변환
