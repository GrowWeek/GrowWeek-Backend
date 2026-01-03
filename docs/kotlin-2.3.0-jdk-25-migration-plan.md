# Kotlin 2.3.0 & JDK 25 마이그레이션 계획

## 1. 현재 상태

| 항목 | 현재 버전 | 목표 버전 |
|------|----------|----------|
| Kotlin | 2.2.21 | 2.3.0 |
| JDK | 24 | 25 |
| Gradle | 9.2.1 | 유지 |
| Spring Boot | 4.0.0 | 유지 |
| Spring Cloud | 2025.1.0 | 유지 |

## 2. 마이그레이션 대상 파일

### 2.1 빌드 설정
- `server/build.gradle.kts` - Kotlin 버전 및 JDK toolchain 설정

### 2.2 Docker 설정
- `server/Dockerfile` - 베이스 이미지 및 JAVA_HOME 환경변수

### 2.3 CI/CD 워크플로우
- `.github/workflows/server-test.yml` - JDK 버전
- `.github/workflows/deploy-dev.yml` - JDK 버전 (3개 job에서 사용)

---

## 3. 단계별 마이그레이션 절차

### Step 1: build.gradle.kts 수정

**변경 사항:**
```kotlin
// Before
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    // ...
    kotlin("kapt") version "2.2.21"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

// After
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    // ...
    kotlin("kapt") version "2.3.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

### Step 2: Dockerfile 수정

**변경 사항:**
```dockerfile
# Before
FROM openjdk:24-ea-oraclelinux9
ENV JAVA_HOME=/opt/jdk-24

# After
FROM openjdk:25-oraclelinux9
ENV JAVA_HOME=/opt/jdk-25
```

> **참고**: JDK 25가 정식 출시되면 `-ea` (Early Access) 태그가 제거됩니다.
> 이미지 태그는 Docker Hub에서 실제 사용 가능한 태그로 확인 후 적용해야 합니다.

### Step 3: GitHub Actions 워크플로우 수정

#### 3.1 server-test.yml
```yaml
# Before
- name: Set up JDK 24
  uses: actions/setup-java@v4
  with:
    java-version: '24'
    distribution: 'temurin'

# After
- name: Set up JDK 25
  uses: actions/setup-java@v4
  with:
    java-version: '25'
    distribution: 'temurin'
```

#### 3.2 deploy-dev.yml
동일한 변경을 3개 job(test, build-and-push, 미래 추가될 job)에 적용

---

## 4. 호환성 확인 사항

### 4.1 Gradle 호환성
- Gradle 9.2.1은 JDK 25를 지원하는지 확인 필요
- [Gradle 공식 호환성 매트릭스](https://docs.gradle.org/current/userguide/compatibility.html) 참조
- 필요시 Gradle 버전 업그레이드

### 4.2 의존성 호환성
다음 라이브러리들의 JDK 25 호환성 확인:

| 라이브러리 | 현재 버전 | JDK 25 호환 여부 |
|-----------|----------|-----------------|
| Spring Boot | 4.0.0 | 확인 필요 |
| Spring Cloud | 2025.1.0 | 확인 필요 |
| Exposed ORM | 1.0.0-rc-4 | 확인 필요 |
| Kotest | 6.0.7 | 확인 필요 |
| JaCoCo | 0.8.14 | 확인 필요 |
| ktlint plugin | 14.0.1 | 확인 필요 |

### 4.3 Temurin JDK 25 가용성
- GitHub Actions의 `actions/setup-java@v4`에서 Temurin JDK 25 지원 여부 확인
- 미지원시 대안 distribution 선택 (Oracle, Corretto 등)

---

## 5. Kotlin 2.3.0 주요 변경사항

### 5.1 언어 기능 변화
- Kotlin 2.3.0 릴리스 노트 확인 후 breaking changes 적용
- deprecated API 사용 여부 점검

### 5.2 컴파일러 옵션
현재 사용 중인 컴파일러 옵션:
```kotlin
freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
```
- 해당 옵션이 Kotlin 2.3.0에서도 유효한지 확인

---

## 6. 마이그레이션 체크리스트

### 사전 준비
- [ ] Kotlin 2.3.0 릴리스 노트 확인
- [ ] JDK 25 릴리스 노트 확인
- [ ] Gradle JDK 25 호환성 확인
- [ ] 주요 의존성 JDK 25 호환성 확인
- [ ] Docker Hub에서 openjdk:25 이미지 태그 확인
- [ ] GitHub Actions에서 JDK 25 distribution 지원 확인

### 구현
- [ ] `server/build.gradle.kts` 수정
  - [ ] Kotlin 버전 2.2.21 → 2.3.0
  - [ ] JDK toolchain 24 → 25
- [ ] `server/Dockerfile` 수정
  - [ ] 베이스 이미지 변경
  - [ ] JAVA_HOME 경로 수정
- [ ] `.github/workflows/server-test.yml` 수정
- [ ] `.github/workflows/deploy-dev.yml` 수정

### 검증
- [ ] 로컬 빌드 테스트 (`./gradlew build`)
- [ ] 로컬 테스트 실행 (`./gradlew test`)
- [ ] Docker 이미지 빌드 테스트
- [ ] CI 파이프라인 정상 동작 확인

---

## 7. 롤백 계획

마이그레이션 실패 시 모든 변경사항을 이전 버전으로 되돌림:
- Kotlin: 2.2.21
- JDK: 24

Git을 통해 이전 커밋으로 revert 가능

---

## 8. 예상 이슈 및 대응

### 8.1 JDK 25 Early Access 단계
- JDK 25가 아직 정식 출시되지 않았을 경우, EA 버전 사용 고려
- 프로덕션 환경에서는 GA 출시 후 적용 권장

### 8.2 Docker 이미지 미지원
- `openjdk:25` 이미지가 없을 경우:
  - Eclipse Temurin 이미지 사용: `eclipse-temurin:25-jdk`
  - Amazon Corretto 이미지 사용: `amazoncorretto:25`

### 8.3 GitHub Actions JDK 25 미지원
- Temurin이 JDK 25를 미지원할 경우:
  - `distribution: 'oracle'` 또는 `distribution: 'corretto'` 사용
  - 또는 수동 JDK 설치 스크립트 추가

---

## 9. 참고 자료

- [Kotlin 2.3.0 릴리스 노트](https://kotlinlang.org/docs/whatsnew23.html)
- [JDK 25 릴리스 정보](https://openjdk.org/projects/jdk/25/)
- [Gradle 호환성 매트릭스](https://docs.gradle.org/current/userguide/compatibility.html)
- [actions/setup-java 지원 버전](https://github.com/actions/setup-java)
