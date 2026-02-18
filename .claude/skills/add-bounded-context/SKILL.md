---
name: add-bounded-context
description: 새로운 Bounded Context(도메인)를 DDD, Clean Architecture, CQRS 패턴에 맞게 추가합니다. 새로운 도메인이나 모듈을 추가할 때 사용하세요.
---

# Add Bounded Context

## Instructions

새로운 도메인을 위한 Bounded Context를 생성할 때 다음 단계를 따르세요:

### 1. 디렉토리 구조 생성

`server/src/main/kotlin/xyz/robinjoon/growweek/{bounded-context}/` 경로에 다음 구조를 생성합니다:

```
{bounded-context}/
├── presentation/
│   └── rest/
│       ├── request/
│       ├── response/
│       └── controller/
├── application/
│   ├── command/
│   ├── dto/
│   ├── query/
│   ├── service/
│   └── usecase/
├── domain/
│   ├── model/
│   │   ├── command/
│   │   └── query/
│   ├── repository/
│   └── service/
└── infrastructure/
    ├── persistence/
    └── external/
```

### 2. 네이밍 규칙

- Bounded Context 이름은 소문자 사용 (예: `task`, `user`, `notification`)
- 패키지명은 도메인 용어를 사용
- 단수형 사용 (예: `task`, `user`)

### 3. 각 레이어의 역할

- **presentation/**: REST API Controllers, Request/Response DTOs (rest/ 하위에 구성)
- **application/**: Use Cases 인터페이스(usecase/), Application Services 구현체(service/), Command/Query DTOs, Response DTOs(dto/)
- **domain/**: 도메인 모델, Repository 인터페이스, Domain Services
- **infrastructure/**: Exposed ORM, Repository 구현, OpenFeign 클라이언트

### 4. 아키텍처 테스트 업데이트

새 Bounded Context를 추가하면 `server/src/test/kotlin/xyz/robinjoon/growweek/architecture/ArchitectureTest.kt`에 BC 격리 규칙을 반드시 추가해야 합니다.

1. `BOUNDED_CONTEXTS` 리스트에 새 BC 이름 추가
2. 새 BC의 `@ArchTest` 격리 규칙 추가

```kotlin
// 1. 리스트에 추가
private val BOUNDED_CONTEXTS = listOf("member", "task", "retrospective", "newContext")

// 2. 격리 규칙 추가
@ArchTest
val `BC 간 직접 참조가 없어야 한다 - newContext`: ArchRule =
    noClasses()
        .that()
        .resideInAPackage("$BASE_PACKAGE.newContext..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            *BOUNDED_CONTEXTS
                .filter { it != "newContext" }
                .map { "$BASE_PACKAGE.$it.." }
                .toTypedArray(),
        )
```

상세 규칙은 `docs/architecture-test.md`를 참고하세요.

### 5. 주의사항

- 의존성 방향: Presentation → Application → Domain ← Infrastructure
- Domain Layer는 외부 의존성 없이 순수 Kotlin 코드로 작성
- CQRS 패턴에 따라 Command와 Query 모델 분리

## Examples

### 예시: User Bounded Context 생성

```bash
# 디렉토리 구조 생성
mkdir -p server/src/main/kotlin/xyz/robinjoon/growweek/user/{presentation/rest/{request,response,controller},application/{command,dto,query,service,usecase},domain/{model/{command,query},repository,service},infrastructure/{persistence,external}}
```

### 기본 패키지 구조

```kotlin
// server/src/main/kotlin/xyz/robinjoon/growweek/user/domain/model/command/User.kt
package xyz.robinjoon.growweek.user.domain.model.command

data class User(
    val id: UserId,
    val email: Email,
    val name: String
)
```
