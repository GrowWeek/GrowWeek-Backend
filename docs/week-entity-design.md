# Week 엔티티 설계 분석

## 배경

`period-model-complexity-analysis.md`에서 식별된 문제를 해결하기 위해 Week 엔티티 도입을 검토합니다. 핵심 질문은 다음과 같습니다:

1. Week는 단순한 엔티티로 충분한가?
2. Week는 어떤 Bounded Context에 속해야 하는가?

---

## 현재 BC 구조

```
GrowWeek-Backend/
├── common/          # Shared Kernel (MemberId, TaskId, RetrospectiveId, 이벤트 인프라)
├── member/          # Member BC
├── task/            # Task BC (Task, TaskPeriod, CompletedRetrospectivePeriod)
└── retrospective/   # Retrospective BC (Retrospective, RetrospectivePeriod)
```

**현재 "주간" 개념의 위치:**
- Task BC: `TaskPeriod(startDate, dueDate)`
- Retrospective BC: `RetrospectivePeriod(startDate, endDate)`
- 두 BC가 각각 독립적으로 기간을 관리

---

## Week가 가져야 할 책임 분석

### 현재 분산된 "주간" 관련 로직

| 로직 | 현재 위치 | Week로 이동 가능 여부 |
|------|----------|---------------------|
| 주간 시작일/종료일 정의 | 각 BC에서 개별 관리 | O - Week의 핵심 속성 |
| 회고 작성 가능 기간 계산 | `RetrospectivePeriod.isWritable()` | O - Week의 비즈니스 규칙 |
| 기간 겹침 판별 | 4곳에 분산 | O - Week 비교로 대체 |
| Task의 주간 소속 판별 | `Task.belongsToWeek()` | O - `task.weekId` 비교로 단순화 |
| 완료된 회고 기간 추적 | `CompletedRetrospectivePeriod` | △ - 부분적 단순화 |

### Week가 담당해야 할 핵심 책임

1. **주간의 경계 정의**: 시작일(월요일)과 종료일(일요일)
2. **회고 작성 가능 기간 계산**: 금요일 ~ 다음 월요일
3. **주간 간 순서/비교**: 이전 주, 다음 주, 현재 주 판별
4. **주간 식별**: 고유 ID 또는 자연키(year + weekNumber)

---

## 설계 옵션 비교

### 옵션 1: Common(Shared Kernel)에 Week 값 객체 추가

```kotlin
// common/domain/model/Week.kt
data class Week(
    val year: Int,
    val weekNumber: Int,  // ISO 주차
) {
    val startDate: LocalDate  // 계산됨
    val endDate: LocalDate    // 계산됨

    fun isWritablePeriod(currentDate: LocalDate): Boolean
    fun contains(date: LocalDate): Boolean

    companion object {
        fun of(date: LocalDate): Week  // 특정 날짜가 속한 주
        fun current(): Week            // 현재 주
    }
}
```

**장점:**
- 모든 BC가 동일한 Week 개념 공유
- 별도 저장소 불필요 (계산으로 생성)
- 가장 단순한 구현

**단점:**
- Week별 커스텀 설정 불가 (예: 특정 주만 회고 기간 연장)
- 값 객체이므로 영속화 개념 없음

**적합한 경우:**
- Week가 순수하게 "시간 범위"만 표현할 때
- 모든 주가 동일한 규칙을 따를 때

---

### 옵션 2: 독립 Week BC 생성

```
week/
├── domain/
│   ├── model/
│   │   ├── Week.kt           # 애그리거트
│   │   ├── WeekId.kt
│   │   └── WeekPeriod.kt
│   └── repository/
│       └── WeekRepository.kt
├── application/
│   └── service/
│       ├── GetWeekService.kt
│       └── CreateWeekService.kt
└── infrastructure/
    └── persistence/
        └── ExposedWeekRepository.kt
```

```kotlin
// week/domain/model/Week.kt
data class Week(
    val id: WeekId,
    val year: Int,
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val writableStartDate: LocalDate,   // 커스텀 가능
    val writableEndDate: LocalDate,     // 커스텀 가능
    val status: WeekStatus,             // OPEN, CLOSED, ARCHIVED
) {
    fun isWritable(currentDate: LocalDate): Boolean
    fun canCreateTask(): Boolean
    fun canModifyTask(): Boolean
}

enum class WeekStatus {
    OPEN,      // 진행 중
    CLOSED,    // 회고 완료
    ARCHIVED   // 보관됨
}
```

**장점:**
- Week별 커스텀 설정 가능
- Week의 상태(OPEN/CLOSED) 관리 가능
- 회고 완료 → Week 상태 변경으로 단순화
- 확장성 높음 (Week별 메타데이터 추가 가능)

**단점:**
- BC가 하나 더 추가됨 (복잡도 증가)
- Task, Retrospective가 Week BC에 의존
- Week 생성/관리 로직 필요

**적합한 경우:**
- Week가 독립적인 라이프사이클을 가질 때
- Week별로 다른 규칙이 필요할 때
- "주간 단위 계획" 같은 기능이 추가될 때

---

### 옵션 3: Retrospective BC에 Week 엔티티 추가

```kotlin
// retrospective/domain/model/Week.kt
data class Week(
    val id: WeekId,
    val year: Int,
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val retrospectiveId: RetrospectiveId?,  // 1:1 관계
    val status: WeekStatus,
) {
    fun isWritable(currentDate: LocalDate): Boolean
}

// retrospective/domain/model/Retrospective.kt
data class Retrospective(
    val id: RetrospectiveId,
    val weekId: WeekId,  // period 대신
    val memberId: MemberId,
    // ...
)
```

**장점:**
- "회고는 주간 단위"라는 도메인 개념과 일치
- Retrospective와 Week가 함께 관리됨
- 회고 완료 시 Week 상태 변경 용이

**단점:**
- Task BC가 Retrospective BC의 Week에 의존 → BC 간 결합도 증가
- Week가 회고 중심으로만 설계될 위험

**적합한 경우:**
- 회고가 시스템의 핵심 기능일 때
- Task가 회고에 종속적인 관계일 때

---

### 옵션 4: Task BC에 Week 엔티티 추가

```kotlin
// task/domain/model/Week.kt
data class Week(
    val id: WeekId,
    val memberId: MemberId,
    val year: Int,
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val retrospectiveCompleted: Boolean,
) {
    fun canAddTask(): Boolean = !retrospectiveCompleted
    fun canModifyTask(): Boolean = !retrospectiveCompleted
}

// task/domain/model/Task.kt
data class Task(
    val id: TaskId,
    val weekId: WeekId,  // period 대신
    // ...
)
```

**장점:**
- Task의 주간 소속이 명확해짐
- `CompletedRetrospectivePeriod` 제거 가능 → Week.retrospectiveCompleted로 대체
- Task 검증 로직 단순화

**단점:**
- Retrospective BC가 Task BC의 Week에 의존
- Week가 Task 중심으로만 설계될 위험

**적합한 경우:**
- Task 관리가 시스템의 핵심 기능일 때

---

## 옵션별 영향도 비교

| 기준 | 옵션 1 (Common) | 옵션 2 (독립 BC) | 옵션 3 (Retro BC) | 옵션 4 (Task BC) |
|------|----------------|-----------------|------------------|-----------------|
| 구현 복잡도 | 낮음 | 높음 | 중간 | 중간 |
| BC 간 결합도 | 낮음 | 중간 | 높음 | 높음 |
| 확장성 | 낮음 | 높음 | 중간 | 중간 |
| 기간 로직 단순화 | O | O | O | O |
| Week별 커스텀 | X | O | O | O |
| Week 상태 관리 | X | O | O | O |
| 기존 코드 변경량 | 적음 | 많음 | 중간 | 중간 |

---

## 추천: 옵션 2 (독립 Week BC) 또는 옵션 1 (Common Week 값 객체)

### 단기적으로: 옵션 1 (Common Week 값 객체)

현재 요구사항이 단순하다면 Common에 Week 값 객체를 추가하는 것이 가장 실용적입니다.

```kotlin
// common/domain/model/Week.kt
@JvmInline
value class WeekId(val value: String) {
    companion object {
        fun of(year: Int, weekNumber: Int): WeekId = WeekId("$year-W${weekNumber.toString().padStart(2, '0')}")
        fun of(date: LocalDate): WeekId {
            val weekFields = WeekFields.ISO
            val year = date.get(weekFields.weekBasedYear())
            val week = date.get(weekFields.weekOfWeekBasedYear())
            return of(year, week)
        }
    }
}

data class Week(
    val id: WeekId,
    val year: Int,
    val weekNumber: Int,
) {
    val startDate: LocalDate by lazy {
        LocalDate.of(year, 1, 1)
            .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNumber.toLong())
            .with(DayOfWeek.MONDAY)
    }

    val endDate: LocalDate by lazy { startDate.plusDays(6) }

    val writableStartDate: LocalDate by lazy { endDate.minusDays(2) }  // 금요일

    val writableEndDate: LocalDate by lazy {
        endDate.plusDays(1)  // 다음 월요일 0시 전까지
    }

    fun isWritable(currentDate: LocalDate = LocalDate.now()): Boolean =
        !currentDate.isBefore(writableStartDate) && currentDate.isBefore(writableEndDate)

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)

    companion object {
        fun of(date: LocalDate): Week {
            val weekFields = WeekFields.ISO
            val year = date.get(weekFields.weekBasedYear())
            val weekNumber = date.get(weekFields.weekOfWeekBasedYear())
            return Week(WeekId.of(year, weekNumber), year, weekNumber)
        }

        fun current(): Week = of(LocalDate.now())
    }
}
```

**변경 후 Task, Retrospective:**

```kotlin
// Task BC
data class Task(
    val id: TaskId,
    val weekId: WeekId,  // TaskPeriod 대신
    val dueDate: LocalDate,  // 마감일은 유지 (주 내에서의 마감)
    // ...
)

// Retrospective BC
data class Retrospective(
    val id: RetrospectiveId,
    val weekId: WeekId,  // RetrospectivePeriod 대신
    // ...
)
```

**검증 로직 단순화:**

```kotlin
// Before: 복잡한 겹침 계산
fun validateNotInCompletedRetrospectivePeriod(command: CreateTask) {
    val query = CompletedRetrospectivePeriodQuery.byMemberIdAndOverlappingPeriod(
        memberId = command.memberId,
        periodStart = command.startDate,
        periodEnd = command.dueDate
    )
    val overlapping = repository.findAll(query)
    if (overlapping.isNotEmpty()) throw ...
}

// After: WeekId 비교
fun validateNotInCompletedRetrospectiveWeek(command: CreateTask) {
    val weekId = WeekId.of(command.dueDate)
    val isCompleted = completedWeekRepository.existsByMemberIdAndWeekId(
        memberId = command.memberId,
        weekId = weekId
    )
    if (isCompleted) throw IllegalArgumentException("회고가 완료된 주($weekId)에는 할일을 추가할 수 없습니다.")
}
```

---

### 장기적으로: 옵션 2 (독립 Week BC)

다음 요구사항이 생기면 독립 Week BC로 발전시킵니다:

- Week별 목표 설정
- Week별 커스텀 기간 (휴일, 특별 주간)
- Week별 통계/리포트
- 주간 단위 알림/리마인더

```
week/
├── domain/
│   ├── model/
│   │   ├── Week.kt
│   │   ├── WeekId.kt
│   │   └── WeekGoal.kt        # 주간 목표
│   └── repository/
├── application/
│   └── service/
│       ├── GetCurrentWeekService.kt
│       ├── GetWeekHistoryService.kt
│       └── SetWeekGoalService.kt
└── infrastructure/
```

---

## 마이그레이션 전략

### Phase 1: WeekId 도입 (Common)

1. `common/domain/model/Week.kt`, `WeekId.kt` 추가
2. Task, Retrospective 엔티티에 `weekId` 필드 추가 (기존 period와 병행)
3. 새 API에서 weekId 사용 시작

### Phase 2: 기존 period 로직 교체

1. Task 조회 쿼리를 weekId 기반으로 변경
2. Retrospective 조회 쿼리를 weekId 기반으로 변경
3. `CompletedRetrospectivePeriod` → `CompletedWeek`로 단순화

### Phase 3: 레거시 제거

1. `TaskPeriod.overlaps()` 제거
2. `RetrospectivePeriod.isWritable()` → `Week.isWritable()`로 이동
3. `CompletedRetrospectivePeriod` 제거

### Phase 4 (선택): 독립 Week BC 분리

1. Week 관련 로직을 별도 BC로 추출
2. Week 상태 관리 추가
3. Week별 확장 기능 추가

---

## 결론

**Week는 단순한 엔티티로 시작해도 충분합니다.**

- 현재 요구사항: Common에 Week 값 객체로 충분
- WeekId를 공유하면 "같은 주" 판별이 `==` 비교로 단순화됨
- 기간 겹침 로직 4곳 → WeekId 비교 1곳으로 통합

**Week의 소속:**

| 단계 | 위치 | 이유 |
|------|------|------|
| 초기 | Common (Shared Kernel) | 모든 BC가 공유하는 시간 개념 |
| 확장 시 | 독립 Week BC | Week가 고유한 라이프사이클과 비즈니스 규칙을 가질 때 |

Week를 특정 BC(Task나 Retrospective)에 넣으면 다른 BC가 의존하게 되어 결합도가 높아집니다. 공유 개념은 Common에, 확장이 필요하면 독립 BC로 분리하는 것이 DDD 원칙에 부합합니다.
