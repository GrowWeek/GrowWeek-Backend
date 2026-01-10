# 기간(Period) 모델 복잡성 분석

## 개요

현재 코드베이스에서 Task(할일)와 Retrospective(회고)가 "주간"이라는 기간에 속하는 관계를 `startDate`와 `endDate`로 직접 표현하고 있습니다. 이로 인해 발생하는 복잡성을 분석합니다.

## 핵심 문제

**"주간"이라는 명확한 개념이 존재하지 않고, 시작일/종료일 쌍으로만 기간을 표현**

- Task는 `startDate + dueDate`로 기간 표현
- Retrospective는 `startDate + endDate`로 기간 표현
- 두 도메인이 같은 "주간"에 속하는지 확인하려면 날짜 범위 겹침 계산 필요

---

## 발견된 복잡성 근거

### 1. 동일한 겹침 로직의 반복 구현

#### TaskPeriod.kt (라인 17-18)
```kotlin
fun overlaps(weekStart: LocalDate, weekEnd: LocalDate): Boolean =
    !(dueDate.isBefore(weekStart) || startDate.isAfter(weekEnd))
```

#### CompletedRetrospectivePeriod.kt (라인 16-17)
```kotlin
fun overlaps(periodStart: LocalDate, periodEnd: LocalDate): Boolean =
    !periodEnd.isBefore(startDate) && !periodStart.isAfter(endDate)
```

#### ExposedCompletedRetrospectivePeriodRepository.kt (라인 74-75)
```kotlin
(CompletedRetrospectivePeriodTable.startDate lessEq query.periodEnd) and
(CompletedRetrospectivePeriodTable.endDate greaterEq query.periodStart)
```

#### ExposedTaskRepository.kt (라인 175-176)
```kotlin
(TaskTable.startDate lessEq query.weekEnd) and
(TaskTable.dueDate greaterEq query.weekStart)
```

**문제점:**
- 수학적으로 동일한 범위 겹침 로직이 4곳에서 각각 다르게 표현됨
- `isBefore`/`isAfter` vs `lessEq`/`greaterEq` 혼용
- 새로운 기능 추가 시 겹침 로직을 또 구현해야 함

---

### 2. 테스트 케이스 폭발

기간 경계값 검증을 위한 테스트 케이스가 과도하게 많습니다.

| 테스트 파일 | 테스트 케이스 수 | 검증 대상 |
|------------|-----------------|----------|
| TaskPeriodTest.kt | 14개 | 기간 겹침 경계값 |
| RetrospectivePeriodTest.kt | 16개 | 회고 작성 가능 기간 경계값 |

**TaskPeriodTest 예시:**
- 마감일이 주 시작일과 같은 경우 → 겹침
- 마감일이 주 시작일 하루 전인 경우 → 안겹침
- 시작일이 주 종료일과 같은 경우 → 겹침
- 시작일이 주 종료일 하루 후인 경우 → 안겹침
- ... (총 14개)

**문제점:**
- "같은 주에 속한다"는 단순한 개념에 14개의 테스트가 필요
- Week 엔티티가 있었다면 `task.weekId == retrospective.weekId` 단일 비교로 충분

---

### 3. 다층 검증 구조

Task 생성/수정 시 기간 검증이 여러 레이어에 분산되어 있습니다.

```
CreateTaskService.validateNotInCompletedRetrospectivePeriod()
    ↓
CompletedRetrospectivePeriodRepository.findAll(query)
    ↓
ExposedCompletedRetrospectivePeriodRepository (SQL 레벨 겹침 검증)
    ↓
CompletedRetrospectivePeriod.overlaps() (도메인 레벨 겹침 검증)
```

**문제점:**
- 동일한 겹침 검증이 SQL과 도메인 양쪽에서 수행
- 어느 레이어에서 검증해야 하는지 불명확
- 검증 로직 변경 시 여러 곳을 수정해야 함

---

### 4. 복잡한 날짜 계산 로직

#### RetrospectivePeriod.isWritable() - 회고 작성 가능 여부

```kotlin
fun isWritable(currentDate: LocalDate = LocalDate.now()): Boolean {
    val writableStartDate = endDate.minusDays(2)  // 종료일 2일 전 (금요일)
    val writableEndDate = calculateNextMonday(endDate)  // 다음 월요일
    return !currentDate.isBefore(writableStartDate) && currentDate.isBefore(writableEndDate)
}

private fun calculateNextMonday(date: LocalDate): LocalDate {
    val daysUntilMonday = (DayOfWeek.MONDAY.value - date.dayOfWeek.value + 7) % 7
    return if (daysUntilMonday == 0) {
        date.plusDays(7)  // 이미 월요일이면 다음 주 월요일
    } else {
        date.plusDays(daysUntilMonday.toLong())
    }
}
```

**문제점:**
- "금요일부터 다음 월요일까지"라는 비즈니스 규칙이 날짜 계산으로 풀어져 있음
- 요일 계산 공식 `(DayOfWeek.MONDAY.value - date.dayOfWeek.value + 7) % 7`이 직관적이지 않음
- 종료일이 월요일인 경우의 특수 처리 필요

---

### 5. 4가지 시간축의 혼재

현재 시스템에서 사용되는 시간 개념들:

| 시간축 | 표현 방식 | 용도 |
|--------|----------|------|
| Task 기간 | startDate + dueDate | 할일 예정 기간 |
| Retrospective 기간 | startDate + endDate | 회고 대상 주간 |
| 회고 작성 가능 기간 | endDate - 2일 ~ 다음 월요일 | 회고 작성 허용 시간 |
| 완료된 회고 기간 | startDate + endDate | Task 생성/수정 제한 |

**문제점:**
- 모두 "주간"을 표현하지만 서로 다른 방식으로 계산
- Task의 startDate는 실제로 거의 사용되지 않음 (대부분 dueDate만 검증)
- "같은 주"인지 확인하려면 각 시간축별로 다른 계산 필요

---

### 6. Bounded Context 간 데이터 동기화 복잡성

Task BC가 Retrospective BC를 직접 의존하지 않기 위해 `CompletedRetrospectivePeriod` 도메인이 추가되었습니다.

```kotlin
// CompletedRetrospectivePeriod.kt
data class CompletedRetrospectivePeriod(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val startDate: LocalDate,  // Retrospective의 startDate 복사
    val endDate: LocalDate,    // Retrospective의 endDate 복사
    val completedAt: LocalDateTime,
)
```

**문제점:**
- Retrospective의 기간 정보가 Task BC에 복제됨
- 도메인 이벤트로 동기화 필요
- 동일한 기간 데이터가 두 곳에 존재

---

### 7. 상태와 시간의 결합 검증

#### Task.belongsToWeek() (라인 112-125)

```kotlin
fun belongsToWeek(weekStart: LocalDate, weekEnd: LocalDate): Boolean {
    if (!period.overlaps(weekStart, weekEnd)) return false

    // 완료 시점이 마감일 이전인 경우 해당 주에 속하지 않음
    if (status == TaskStatus.DONE && updatedAt.toLocalDate().isBefore(period.dueDate)) {
        return false
    }
    return true
}
```

**문제점:**
- Task 상태(DONE)와 업데이트 시간, 마감일을 함께 고려
- "마감일 이전에 완료된 할일은 다른 주에 속한다"는 암묵적 규칙
- 세 가지 시간(weekStart/weekEnd, updatedAt, dueDate)을 동시에 비교

---

## 근본 원인

### 명시적 Week 엔티티의 부재

현재 구조:
```
Task ──── startDate, dueDate ────┐
                                 │ (날짜 범위 겹침 계산)
Retrospective ── startDate, endDate ─┘
```

Week 엔티티가 있었다면:
```
Task ──────── weekId ────→ Week (id, startDate, endDate)
                              ↑
Retrospective ─ weekId ───────┘
```

**Week 엔티티 도입 시 장점:**
1. `task.weekId == retrospective.weekId` 단일 비교로 같은 주 판별
2. 겹침 계산 로직 불필요
3. Week별 비즈니스 규칙(회고 작성 가능 기간 등)을 Week 엔티티에서 관리
4. 테스트 케이스 단순화

---

## 현재 코드의 복잡도 지표

| 지표 | 값 | 설명 |
|------|-----|------|
| 겹침 로직 구현 횟수 | 4회 | TaskPeriod, CompletedRetrospectivePeriod, 2개의 Repository |
| 기간 관련 테스트 케이스 | 30개+ | TaskPeriodTest 14개 + RetrospectivePeriodTest 16개 |
| 날짜 비교 연산자 사용 | 20회+ | isBefore, isAfter, lessEq, greaterEq |
| 시간축 개념 | 4가지 | Task 기간, Retrospective 기간, 회고 작성 기간, 완료된 회고 기간 |

---

## 결론

"하나의 주간에 할일과 회고가 속한다"는 단순한 도메인 개념이 `startDate`/`endDate` 직접 표현으로 인해:

1. **겹침 계산 로직이 4곳에 분산**
2. **30개 이상의 경계값 테스트 필요**
3. **다층 검증 구조로 인한 유지보수 어려움**
4. **BC 간 기간 데이터 복제**
5. **복잡한 요일 계산 로직**

Week 엔티티 도입을 통해 이러한 복잡성을 크게 줄일 수 있을 것으로 보입니다.
