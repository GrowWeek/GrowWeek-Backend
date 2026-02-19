# common 공유 계약 비대화 방지 구조 재검토

> Issue: [#21 - common 공유 계약 비대화 방지 구조 재검토](https://github.com/robinjoon/GrowWeek-Backend/issues/21)

## 1. 현재 구조 분석

### 1.1 common 패키지 현황

현재 `common/` 패키지는 다음과 같은 카테고리로 구성되어 있다.

```
common/
├── Page.kt                          # 페이징 기반 클래스 (Page, PageQuery, PageInfo 등)
├── config/                           # 공통 설정
│   ├── EventConfig.kt
│   ├── SecurityConfig.kt
│   └── WebMvcConfig.kt
├── domain/                           # 공유 도메인 모델
│   ├── MemberId.kt                  # 범용 식별자 VO
│   ├── TaskId.kt                    # 범용 식별자 VO
│   ├── WeekId.kt                    # 범용 식별자 VO
│   ├── RetrospectiveId.kt           # 범용 식별자 VO
│   ├── SensitivityLevel.kt          # 범용 열거형
│   ├── TaskSummary.kt               # ★ BC 특화 공유 계약
│   ├── TaskSummaryPayload.kt        # ★ BC 특화 공유 계약
│   └── TaskSummaryStatus.kt         # ★ BC 특화 공유 계약
├── event/                            # 도메인 이벤트 기반 인프라
│   ├── DomainEvent.kt
│   ├── DomainEventHandler.kt
│   ├── DomainEventPublisher.kt
│   └── payload/
│       └── RetrospectiveEventPayload.kt  # ★ BC 특화 이벤트 페이로드
├── infrastructure/                    # 공통 인프라 구현체
│   ├── DomainEventDispatcher.kt
│   ├── SpringDomainEventPublisher.kt
│   └── security/
│       ├── CurrentMemberIdArgumentResolver.kt
│       ├── JwtAuthenticationFilter.kt
│       ├── JwtAuthenticationToken.kt
│       └── JwtTokenProvider.kt
├── port/                              # BC 간 통신 포트
│   ├── MemberTokenPort.kt           # member → common 포트
│   └── TaskSummaryPort.kt           # ★ task → retrospective 공유 포트
└── presentation/
    └── security/
        └── CurrentMemberId.kt
```

### 1.2 구성 요소 분류

| 분류 | 요소 | 성격 | 변경 빈도 |
|------|------|------|----------|
| **범용 기반(Kernel)** | `Page`, `PageQuery`, `PageInfo` | 모든 BC가 동일하게 사용하는 기반 추상화 | 매우 낮음 |
| **범용 식별자** | `MemberId`, `TaskId`, `WeekId`, `RetrospectiveId` | 여러 BC에서 참조하는 ID Value Object | 낮음 |
| **범용 열거형** | `SensitivityLevel` | 여러 BC에서 사용하는 공통 열거형 | 낮음 |
| **기반 인프라** | `DomainEvent`, `DomainEventHandler`, `DomainEventPublisher`, `DomainEventDispatcher` | 이벤트 시스템 프레임워크 | 매우 낮음 |
| **공통 설정** | `SecurityConfig`, `EventConfig`, `WebMvcConfig`, JWT 관련 | 애플리케이션 횡단 관심사 | 낮음 |
| **BC 특화 계약** | `TaskSummary`, `TaskSummaryPayload`, `TaskSummaryStatus`, `TaskSummaryPort` | task BC의 데이터를 retrospective가 소비하기 위한 계약 | **중간** |
| **BC 특화 이벤트** | `RetrospectiveEventPayload` | retrospective → task 이벤트 통신 계약 | **중간** |
| **BC 특화 포트** | `MemberTokenPort` | member BC의 토큰 생성 기능을 외부에 노출하는 포트 | **중간** |

### 1.3 각 BC의 common 의존 관계

**member BC:**
- `common.domain`: `MemberId` (자체 식별자)
- `common.port`: `MemberTokenPort` (LoginService에서 사용)
- `common.presentation.security`: `CurrentMemberId` (컨트롤러 어노테이션)
- `common`: `Page`, `PageQuery` 등 (페이징)

**task BC:**
- `common.domain`: `MemberId`, `TaskId`, `WeekId`, `RetrospectiveId`, `SensitivityLevel`, `TaskSummary*` (도메인 모델 필드)
- `common.port`: `TaskSummaryPort` (TaskSummaryPortAdapter가 구현)
- `common.event`: `DomainEvent`, `DomainEventHandler`, `RetrospectiveEventPayload` (이벤트 수신)
- `common`: `Page`, `PageQuery` 등 (페이징)

**retrospective BC:**
- `common.domain`: `MemberId`, `WeekId`, `RetrospectiveId`, `SensitivityLevel`, `TaskSummary`, `TaskSummaryPayload` (도메인 모델 필드 + TaskSummary 소비)
- `common.port`: `TaskSummaryPort` (GenerateQuestionsService에서 의존)
- `common.event`: `DomainEventPublisher`, `RetrospectiveEventPayload` (이벤트 발행)
- `common`: `Page`, `PageQuery` 등 (페이징)

### 1.4 현재 구조의 강점

1. **BC 간 직접 참조 완전 차단**: ArchUnit 테스트로 자동 검증
2. **동기 이벤트 기반 통신**: `DomainEventPublisher` → `DomainEventDispatcher` → `DomainEventHandler` 체인으로 단일 트랜잭션 유지
3. **Port-Adapter 패턴**: `TaskSummaryPort`(common) / `TaskSummaryPortAdapter`(task infra) 구조로 retrospective의 task 직접 참조 제거 성공

### 1.5 현재 구조의 위험 요소

1. **BC 특화 계약의 common 집적**: `TaskSummary*`, `RetrospectiveEventPayload`, `MemberTokenPort` 등 특정 BC 간 계약이 `common`에 축적됨
2. **소유권 모호성**: `TaskSummary`는 task BC의 개념이지만 common에 위치하여 누가 변경 책임을 지는지 불명확
3. **변경 전파 리스크**: BC 특화 계약 변경 시 common을 거쳐야 하므로 모든 BC에 재컴파일/재배포 영향
4. **네임스페이스 오염**: BC가 늘어날수록 `common/domain`에 서로 관련 없는 계약 모델이 공존

---

## 2. 설계 전략 제안

### 2.1 핵심 원칙: "Shared Kernel은 최소로, BC 계약은 네임스페이스로 분리"

common 패키지를 **두 가지 역할**로 명확히 구분한다:

| 역할 | 위치 | 내용 | 변경 기준 |
|------|------|------|----------|
| **Shared Kernel** | `common/` (루트 및 기존 패키지) | 모든 BC가 동일하게 사용하는 범용 요소 | 전체 합의 필요 |
| **BC 간 공유 계약** | `common/contract/{consumer}-{provider}/` | 특정 BC 간 통신에 필요한 계약 모델/포트 | 관련 BC 간 합의 |

### 2.2 제안 패키지 구조

```
common/
├── Page.kt                                    # Shared Kernel
├── config/                                     # Shared Kernel (횡단 관심사)
│   ├── EventConfig.kt
│   ├── SecurityConfig.kt
│   └── WebMvcConfig.kt
├── domain/                                     # Shared Kernel (범용 식별자/VO)
│   ├── MemberId.kt
│   ├── TaskId.kt
│   ├── WeekId.kt
│   ├── RetrospectiveId.kt
│   └── SensitivityLevel.kt
├── event/                                      # Shared Kernel (이벤트 프레임워크)
│   ├── DomainEvent.kt
│   ├── DomainEventHandler.kt
│   └── DomainEventPublisher.kt
├── infrastructure/                              # Shared Kernel (공통 인프라)
│   ├── DomainEventDispatcher.kt
│   ├── SpringDomainEventPublisher.kt
│   └── security/
│       ├── CurrentMemberIdArgumentResolver.kt
│       ├── JwtAuthenticationFilter.kt
│       ├── JwtAuthenticationToken.kt
│       └── JwtTokenProvider.kt
├── presentation/                                # Shared Kernel (공통 프레젠테이션)
│   └── security/
│       └── CurrentMemberId.kt
└── contract/                                    # ★ BC 간 공유 계약 (신규)
    ├── task/                                    # task BC가 제공하는 계약
    │   ├── TaskSummary.kt                      # task → retrospective 소비용 요약 모델
    │   ├── TaskSummaryPayload.kt               # 요약 조회 요청 파라미터
    │   ├── TaskSummaryStatus.kt                # 요약 상태 열거형
    │   └── TaskSummaryPort.kt                  # task 데이터 조회 포트
    ├── retrospective/                           # retrospective BC가 제공하는 계약
    │   └── RetrospectiveEventPayload.kt        # 회고 이벤트 페이로드
    └── member/                                  # member BC가 제공하는 계약
        └── MemberTokenPort.kt                  # 토큰 발급 포트
```

### 2.3 네이밍 컨벤션

| 요소 | 위치 규칙 | 네이밍 규칙 |
|------|----------|------------|
| BC 식별자 (ID VO) | `common/domain/` | `{BcName}Id.kt` |
| 범용 열거형/VO | `common/domain/` | 도메인 언어 그대로 |
| BC 제공 계약 모델 | `common/contract/{provider-bc}/` | `{BcConcept}.kt` |
| BC 제공 포트 | `common/contract/{provider-bc}/` | `{BcConcept}Port.kt` |
| 이벤트 페이로드 | `common/contract/{publisher-bc}/` | `{BcName}EventPayload.kt` |
| 기반 인프라 인터페이스 | `common/event/` | `DomainEvent*.kt` |

### 2.4 전략의 근거

**왜 `common/contract/{provider-bc}/` 인가?**

1. **소유권 명확화**: 디렉토리 이름이 곧 계약 제공자(provider)를 나타냄. `TaskSummary`는 `contract/task/` 아래에 있으므로 task 팀이 변경 책임을 가짐
2. **영향 범위 가시화**: `contract/task/` 내의 변경은 task 데이터를 소비하는 BC(현재 retrospective)에만 영향을 줌
3. **증가 억제**: 새로운 BC 간 계약이 필요할 때 명시적으로 새 디렉토리를 만들어야 하므로, 계약 추가의 비용이 가시적
4. **flat하지 않은 구조**: `common/domain/`에 모든 것을 넣는 것보다 탐색성(discoverability)이 높음

**왜 `common/contract/{consumer}-{provider}/`가 아닌 `common/contract/{provider}/`인가?**

- 하나의 계약을 여러 consumer가 사용할 수 있음 (e.g., task 요약을 retrospective뿐 아니라 미래의 analytics BC도 소비 가능)
- consumer 기준으로 나누면 동일 계약의 중복 정의 위험
- provider 기준이 DDD의 Published Language 패턴에 더 부합

**왜 `common/domain/{bc}/`가 아닌 별도 `contract/` 디렉토리인가?**

- `domain/`에는 "모든 BC가 동등하게 사용하는 Shared Kernel VO"만 둠으로써 역할이 명확
- `contract/`는 "특정 BC 간 통신을 위한 합의된 인터페이스"라는 별도 의미 부여
- 두 가지 성격의 모델이 같은 디렉토리에 섞이면 "이건 Shared Kernel인가, 계약인가?" 판단이 모호해짐

---

## 3. 공유 계약 최소화 기준

### 3.1 common에 추가해도 되는 것 (Shared Kernel)

다음 조건을 **모두** 만족하는 경우에만 `common/domain/`에 추가:

1. **3개 이상의 BC에서 동일한 의미로 사용**하는 요소
2. **변경 빈도가 매우 낮고**, 변경 시 모든 BC가 동시에 적응해야 하는 요소
3. **식별자(ID VO)** 또는 **범용 열거형** 수준의 단순한 타입

예시:
- `MemberId` — member, task, retrospective 모두 사용
- `WeekId` — task, retrospective 모두 사용하며 범용적 시간 단위
- `SensitivityLevel` — task, retrospective에서 사용하는 범용 열거형
- `Page`, `PageQuery` — 모든 BC의 조회 패턴에서 사용

### 3.2 contract로 분리해야 하는 것

다음 중 **하나라도** 해당하면 `common/contract/{provider}/`에 배치:

1. **특정 BC의 내부 도메인 개념을 다른 BC가 소비할 수 있도록 변환한 모델** (e.g., `TaskSummary`)
2. **특정 BC 간 통신 인터페이스** (e.g., `TaskSummaryPort`)
3. **특정 BC가 발행하는 이벤트의 페이로드** (e.g., `RetrospectiveEventPayload`)
4. **2개 이하의 BC 사이에서만 사용**되며, 해당 BC의 내부 변경에 따라 함께 변경될 가능성이 있는 요소

### 3.3 의사 결정 플로차트

```
새로운 공유 요소가 필요한가?
  │
  ├─ ID Value Object인가?
  │   └─ YES → common/domain/{BcName}Id.kt
  │
  ├─ 3개+ BC에서 동일 의미로 사용하는 범용 타입인가?
  │   └─ YES → common/domain/ (Shared Kernel)
  │
  ├─ BC 간 통신(조회/이벤트)을 위한 계약 모델인가?
  │   └─ YES → common/contract/{provider-bc}/
  │
  ├─ BC 간 통신을 위한 포트 인터페이스인가?
  │   └─ YES → common/contract/{provider-bc}/
  │
  └─ 기반 프레임워크(이벤트 시스템, 페이징 등)인가?
      └─ YES → common/event/ 또는 common/ 루트
```

---

## 4. 리팩터링 계획

### 4.1 변경 요약

| 현재 위치 | 이동 위치 | 사유 |
|----------|----------|------|
| `common/domain/TaskSummary.kt` | `common/contract/task/TaskSummary.kt` | task BC 제공 계약 |
| `common/domain/TaskSummaryPayload.kt` | `common/contract/task/TaskSummaryPayload.kt` | task BC 제공 계약 |
| `common/domain/TaskSummaryStatus.kt` | `common/contract/task/TaskSummaryStatus.kt` | task BC 제공 계약 |
| `common/port/TaskSummaryPort.kt` | `common/contract/task/TaskSummaryPort.kt` | task BC 제공 포트 |
| `common/event/payload/RetrospectiveEventPayload.kt` | `common/contract/retrospective/RetrospectiveEventPayload.kt` | retrospective BC 제공 이벤트 |
| `common/port/MemberTokenPort.kt` | `common/contract/member/MemberTokenPort.kt` | member BC 제공 포트 |

### 4.2 이동하지 않는 요소

| 요소 | 현재 위치 유지 | 사유 |
|------|-------------|------|
| `MemberId`, `TaskId`, `WeekId`, `RetrospectiveId` | `common/domain/` | 3개+ BC가 공유하는 범용 식별자 |
| `SensitivityLevel` | `common/domain/` | 범용 열거형 (task + retrospective) |
| `Page`, `PageQuery`, `PageInfo` 등 | `common/` | 모든 BC의 기반 추상화 |
| `DomainEvent`, `DomainEventHandler`, `DomainEventPublisher` | `common/event/` | 이벤트 프레임워크 (Shared Kernel) |
| `DomainEventDispatcher`, `SpringDomainEventPublisher` | `common/infrastructure/` | 이벤트 프레임워크 구현체 |
| `SecurityConfig`, JWT 관련 | `common/config/`, `common/infrastructure/security/` | 횡단 관심사 |

### 4.3 영향 받는 소스 파일

**task BC:**
- `task/infrastructure/external/TaskSummaryPortAdapter.kt` — import 변경 (`common.domain.*` → `common.contract.task.*`, `common.port.*` → `common.contract.task.*`)
- `task/infrastructure/event/RetrospectiveCompletedHandler.kt` — import 변경 (`common.event.payload.*` → `common.contract.retrospective.*`)

**retrospective BC:**
- `retrospective/application/service/GenerateQuestionsService.kt` — import 변경
- `retrospective/application/service/CompleteRetrospectiveService.kt` — import 변경
- `retrospective/domain/service/QuestionGenerationService.kt` — import 변경
- `retrospective/infrastructure/external/MockQuestionGenerationService.kt` — import 변경
- `retrospective/infrastructure/external/gemini/GeminiQuestionGenerationService.kt` — import 변경

**member BC:**
- `member/application/service/LoginService.kt` — import 변경 (`common.port.MemberTokenPort` → `common.contract.member.MemberTokenPort`)

**테스트 파일:**
- `common/event/RetrospectiveCompletedIntegrationTest.kt` — import 변경
- `task/infrastructure/event/RetrospectiveCompletedHandlerTest.kt` — import 변경
- `task/infrastructure/external/TaskSummaryPortAdapterTest.kt` — import 변경

### 4.4 ArchUnit 테스트 변경

기존 ArchUnit 테스트는 `common` 패키지 전체를 레이어 규칙에서 제외하고 있으므로, `common/contract/` 하위 패키지도 동일하게 제외 범위에 포함되어 **ArchUnit 테스트 변경은 불필요**하다.

### 4.5 ARCHITECTURE.md / SKILL.md 갱신 사항

- `common/` 디렉토리 구조 설명에 `contract/` 하위 구조 추가
- BC 간 공유 계약 추가 시 `contract/{provider-bc}/` 에 배치하라는 가이드 추가
- Shared Kernel vs Contract 판단 기준 문서화

### 4.6 비용/위험 평가

| 항목 | 평가 |
|------|------|
| **코드 변경량** | 낮음 — 패키지 이동 + import 변경만 (로직 변경 없음) |
| **빌드 영향** | 없음 — 모노리포 단일 모듈 구조이므로 패키지 이동만으로 충분 |
| **기능 영향** | 없음 — 런타임 동작 변경 없음 |
| **ArchUnit 영향** | 없음 — common 패키지 전체 제외 규칙이 하위 패키지까지 적용 |
| **학습 비용** | 낮음 — 디렉토리 구조만 이해하면 됨 |

---

## 5. 향후 확장 시나리오

### 5.1 새로운 BC (예: analytics)가 task 데이터를 소비하는 경우

`common/contract/task/`에 이미 `TaskSummary`와 `TaskSummaryPort`가 있으므로, analytics BC는 동일한 계약을 재사용한다. 필요 시 새로운 포트(e.g., `TaskStatisticsPort`)를 `contract/task/`에 추가.

### 5.2 새로운 BC 간 통신 (예: member → retrospective 알림)

```
common/contract/member/
├── MemberTokenPort.kt          # 기존
└── MemberNotificationPayload.kt  # 신규 이벤트 계약
```

### 5.3 계약이 더 이상 필요 없어진 경우

해당 `contract/{provider}/` 디렉토리의 파일을 삭제하면 끝. Shared Kernel에 섞여 있을 때보다 영향 범위 파악이 쉽다.

---

## 6. 결론

| 질문 | 답변 |
|------|------|
| common/domain 중심 확장 vs contract 네임스페이스 분리? | **`common/contract/{provider-bc}/` 네임스페이스 분리** 채택. Shared Kernel은 범용 요소만 유지 |
| Port 위치? | **`common/contract/{provider-bc}/`에 해당 포트 배치**. 계약 모델과 포트를 같은 네임스페이스에 응집 |
| ACL/Port-Adapter 시 소유권/버전 관리? | provider BC 이름이 디렉토리명이므로 **provider에게 변경 책임** 부여. 변경 시 consumer BC와 합의 |
| BC 간 공유 계약 최소화 기준? | 3.1~3.3절의 기준 적용: **3개+ BC 공유 = Shared Kernel, 그 외 = contract** |
| 리팩터링 필요 여부? | **필요함**. 패키지 이동 + import 변경만으로 완료 가능 (로직 변경 없음) |
