# 아키텍처 자동 검증

패키지 의존성 규칙을 CI 수준에서 자동 검증하여, 동일한 아키텍처 위반이 재발하지 않도록 합니다.

## 도구

- **[ArchUnit](https://www.archunit.org/)** 1.4.1 (JUnit5 통합)
- 컴파일된 바이트코드를 분석하여 실제 의존 관계를 검증합니다.

## 실행 방법

```bash
# 아키텍처 테스트만 실행
./gradlew test --tests "xyz.robinjoon.growweek.architecture.ArchitectureTest"

# 전체 테스트 실행 (아키텍처 테스트 포함)
./gradlew test
```

## 검증 규칙

### 1. 계층 의존 방향 검증

Clean Architecture 의존성 방향을 강제합니다.

| 레이어 | 허용된 접근자 |
|--------|-------------|
| Presentation | Infrastructure |
| Application | Presentation, Infrastructure |
| Domain | Application, Infrastructure |
| Infrastructure | 없음 (최하위) |

**예외:**
- `common` 패키지는 공유 모듈이므로 레이어 규칙에서 제외됩니다.
- Application DTO가 domain 타입(enum, value class)을 필드로 노출하므로, Presentation이 DTO 변환 시 `domain.model`에 bytecode 의존이 발생합니다. 이는 아키텍처 문서에서 허용하는 패턴이며, `domain.service`나 `domain.repository` 의존은 여전히 금지됩니다.

### 2. BC 간 직접 참조 금지

각 Bounded Context(member, task, retrospective)는 다른 BC를 직접 참조할 수 없습니다.

- `common` 패키지 의존은 허용됩니다.
- BC 간 통신은 도메인 이벤트(`common.event`)를 통해서만 가능합니다.

### 3. Domain 순수성

`domain` 패키지 내 클래스는 외부 프레임워크에 의존하지 않아야 합니다.

- `org.springframework..` 금지
- `jakarta..` 금지

## 새 Bounded Context 추가 시

새로운 BC를 추가하면 `ArchitectureTest.kt`에 해당 BC의 격리 규칙을 추가해야 합니다.

```kotlin
// BOUNDED_CONTEXTS 리스트에 새 BC 추가
private val BOUNDED_CONTEXTS = listOf("member", "task", "retrospective", "newContext")

// 새 BC의 격리 규칙 추가
@ArchTest
val `BC 간 직접 참조가 없어야 한다 - newContext`: ArchRule =
    noClasses()
        .that().resideInAPackage("$BASE_PACKAGE.newContext..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            *BOUNDED_CONTEXTS.filter { it != "newContext" }
                .map { "$BASE_PACKAGE.$it.." }.toTypedArray(),
        )
```

## 테스트 파일 위치

```
server/src/test/kotlin/xyz/robinjoon/growweek/architecture/ArchitectureTest.kt
```
