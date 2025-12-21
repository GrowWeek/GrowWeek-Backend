# 회고(Retrospective) 기능 구현 계획

## 1. 개요

### 1.1 목적
주간 할일 데이터를 기반으로 AI가 질문을 생성하고, 사용자가 답변을 작성하여 스스로 회고할 수 있는 기능을 제공합니다.

### 1.2 핵심 설계 결정사항

**도메인 관계 설계: 공유 Value Objects 패턴 적용**

회고 기능은 할일 목록과 밀접한 관계가 있으므로, Task와 Retrospective 도메인 간 순환 참조를 방지하고 명확한 의존성을 유지하기 위해 **Shared Kernel 패턴**을 적용합니다.

- **TaskId, RetrospectiveId, SensitivityLevel**을 `common/domain` 패키지로 이동
- 두 도메인이 공유 VO를 통해 서로를 참조
- 순환 참조 없는 깔끔한 의존성 구조

```
Task Domain ←→ common/domain (공유 VO) ←→ Retrospective Domain
```

### 1.3 참고 문서
- [노션 - 전체 플로우](https://www.notion.so/robinjoon/2cb26f51b6c080f298c8cb701bdbe7de)
- [노션 - 회고 비즈니스 규칙](https://www.notion.so/robinjoon/2cb26f51b6c080729cf6e6cdb4961ea8)

## 2. 비즈니스 요구사항

### 2.1 전체 플로우
1. 매주 할일 작성
2. 매일 할일 추가 작성 및 칸반차트 이동 및 추가 코멘트 작성
3. 매주 금요일에 그 주 회고 작성
   - 회고 질문이 할일 목록들 및 상태를 보고 AI에 의해 생성
   - 각 할일에는 민감한 할일 표시가 가능 (민감도에 따라 AI에 전달되는 정보 제한)
   - 질문 생성 시간 동안 대기 후 생성된 회고 질문에 대한 답변 작성
4. 매월 회고를 모아서 볼 수 있음

### 2.2 회고 상태 종류
- `TODO`: 최초 상태
- `BEFORE_GENERATE_QUESTION`: 질문 생성 전
- `AFTER_GENERATE_QUESTION`: 질문 생성 후, 답변 작성 전
- `IN_PROGRESS`: 질문 생성 후, 답변 작성 중
- `DONE`: 회고 완료

### 2.3 회고 데이터 구조
1. **질문**
   - 개수 제약: 최소 2개, 최대 7개 (사용자 입력 가능, 기본값 3개)

2. **답변**
   - 질문과 1:1 대응
   - 답변을 안할 수 있음 (선택적)

3. **기타 회고 내용**
   - 길이 제한: 3000자

4. **기간 정보**
   - 시작일
   - 종료일

5. **회원 uuid**

### 2.4 회고 작성 규칙
1. 회고 작성은 매 주 월요일 0시 0분까지 작성 가능 (향후 기간 설정이 가능할 경우 다음 주차 시작일 0시 0분까지)
   - 질문이 이미 생성된 경우에도 마찬가지
   - 일부 질문에만 답변을 한 경우에도 마찬가지
2. 일부 질문에만 작성 가능
3. 답변 작성은 질문 순서대로 하지 않아도 가능

### 2.5 민감도(SensitivityLevel)에 따른 AI 정보 제한
- **NONE**: 모든 정보 전달
- **TITLE_ONLY**: 제목만 전달
- **HIDDEN**: 전달하지 않음

## 3. 도메인 모델 설계

### 3.1 Bounded Context
`retrospective` 도메인으로 분리하여 구현하되, Task 도메인과 밀접한 관계를 맺음

### 3.2 도메인 간 관계 및 공유 컴포넌트

회고는 할일 목록을 기반으로 생성되므로, 두 도메인 간 관계를 명확히 하고 순환 참조를 방지하기 위해 **공유 식별자 및 VO를 common 패키지로 이동**합니다.

#### 3.2.1 Common으로 이동할 Value Objects
```
common/domain/
  ├── UserId.kt                  (기존)
  ├── TaskId.kt                  (task/domain/model에서 이동)
  ├── RetrospectiveId.kt         (task/domain/model에서 이동)
  └── SensitivityLevel.kt        (task/domain/model에서 이동)
```

**이동 이유:**
- **TaskId**: Retrospective가 Task를 참조하기 위해 필요
- **RetrospectiveId**: Task가 Retrospective를 참조하기 위해 필요 (양방향 참조)
- **SensitivityLevel**: 회고 질문 생성 시 Task의 민감도에 따라 필터링하므로 공유 필요

#### 3.2.2 도메인 간 의존성 방향
```
Task Domain          ←→ (공유 VO) ←→ Retrospective Domain
    ↓                                      ↓
common/TaskId                      common/TaskId
common/RetrospectiveId             common/RetrospectiveId
common/SensitivityLevel            common/SensitivityLevel
```

### 3.3 Aggregate Root
`Retrospective`: 회고 Aggregate의 루트 엔티티

### 3.4 Value Objects (Retrospective 도메인 전용)

#### RetrospectiveId
```kotlin
// common/domain/RetrospectiveId.kt (이동됨)
package xyz.robinjoon.growweek.common.domain

@JvmInline
value class RetrospectiveId(val value: Long) {
    init {
        require(value > 0) { "RetrospectiveId must be greater than 0" }
    }
}
```

#### RetrospectiveStatus
```kotlin
enum class RetrospectiveStatus {
    TODO,                          // 최초 상태
    BEFORE_GENERATE_QUESTION,      // 질문 생성 전
    AFTER_GENERATE_QUESTION,       // 질문 생성 후, 답변 작성 전
    IN_PROGRESS,                   // 질문 생성 후, 답변 작성 중
    DONE                          // 회고 완료
}
```

#### RetrospectivePeriod
```kotlin
data class RetrospectivePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    init {
        require(!startDate.isAfter(endDate)) {
            "시작일은 종료일보다 이전이어야 합니다"
        }
    }

    fun isWritable(currentDate: LocalDate = LocalDate.now()): Boolean {
        // 다음 주 월요일 0시까지 작성 가능
        val nextMonday = endDate.plusDays(1).let {
            it.plusDays((8 - it.dayOfWeek.value) % 7L)
        }
        return !currentDate.isAfter(nextMonday)
    }
}
```

#### QuestionCount
```kotlin
@JvmInline
value class QuestionCount(val value: Int) {
    init {
        require(value in 2..7) {
            "질문 개수는 최소 2개, 최대 7개여야 합니다"
        }
    }

    companion object {
        val DEFAULT = QuestionCount(3)
    }
}
```

#### AdditionalNotes
```kotlin
@JvmInline
value class AdditionalNotes(val value: String) {
    init {
        require(value.length <= 3000) {
            "기타 회고 내용은 3000자를 초과할 수 없습니다"
        }
    }
}
```

### 3.5 Entity

#### Question
```kotlin
data class Question(
    val id: QuestionId,
    val retrospectiveId: RetrospectiveId,
    val content: String,
    val order: Int,
    val createdAt: LocalDateTime
)
```

#### Answer
```kotlin
data class Answer(
    val id: AnswerId,
    val questionId: QuestionId,
    val content: String?,  // null 가능 (답변 선택적)
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### 3.6 Retrospective Aggregate
```kotlin
package xyz.robinjoon.growweek.retrospective.domain.model

import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import java.time.LocalDateTime

data class Retrospective(
    val id: RetrospectiveId,
    val userId: UserId,
    val period: RetrospectivePeriod,
    val status: RetrospectiveStatus,
    val questionCount: QuestionCount,
    val questions: List<Question>,
    val answers: Map<QuestionId, Answer>,
    val additionalNotes: AdditionalNotes?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * 회고 작성 가능 여부 확인
     */
    fun canWrite(): Boolean {
        return period.isWritable() && status != RetrospectiveStatus.DONE
    }

    /**
     * 질문 생성 시작
     */
    fun startGeneratingQuestions(): Retrospective {
        require(status == RetrospectiveStatus.TODO) {
            "질문 생성은 TODO 상태에서만 가능합니다"
        }
        return copy(
            status = RetrospectiveStatus.BEFORE_GENERATE_QUESTION,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 질문 생성 완료
     */
    fun completeQuestionGeneration(generatedQuestions: List<Question>): Retrospective {
        require(status == RetrospectiveStatus.BEFORE_GENERATE_QUESTION) {
            "질문 생성 완료는 BEFORE_GENERATE_QUESTION 상태에서만 가능합니다"
        }
        require(generatedQuestions.size == questionCount.value) {
            "생성된 질문 개수가 설정된 개수와 일치하지 않습니다"
        }
        return copy(
            status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
            questions = generatedQuestions,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 답변 작성/수정
     */
    fun writeAnswer(questionId: QuestionId, content: String?): Retrospective {
        require(canWrite()) {
            "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
        }
        require(questions.any { it.id == questionId }) {
            "존재하지 않는 질문입니다"
        }

        val newAnswer = Answer(
            id = answers[questionId]?.id ?: AnswerId(0), // 새로운 답변이면 Repository에서 ID 생성
            questionId = questionId,
            content = content,
            createdAt = answers[questionId]?.createdAt ?: LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val newStatus = if (status == RetrospectiveStatus.AFTER_GENERATE_QUESTION) {
            RetrospectiveStatus.IN_PROGRESS
        } else {
            status
        }

        return copy(
            status = newStatus,
            answers = answers + (questionId to newAnswer),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 기타 회고 내용 작성
     */
    fun writeAdditionalNotes(notes: AdditionalNotes): Retrospective {
        require(canWrite()) {
            "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
        }
        return copy(
            additionalNotes = notes,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 회고 완료
     */
    fun complete(): Retrospective {
        require(status == RetrospectiveStatus.IN_PROGRESS) {
            "답변을 하나 이상 작성한 후 완료할 수 있습니다"
        }
        return copy(
            status = RetrospectiveStatus.DONE,
            updatedAt = LocalDateTime.now()
        )
    }
}
```

## 4. 계층별 구현 계획

### 4.1 Domain Layer

#### 디렉토리 구조
```
common/domain/
  ├── UserId.kt                  (기존)
  ├── TaskId.kt                  (이동됨)
  ├── RetrospectiveId.kt         (이동됨)
  └── SensitivityLevel.kt        (이동됨)

retrospective/
└── domain/
    ├── model/
    │   ├── Retrospective.kt
    │   ├── Question.kt
    │   ├── Answer.kt
    │   ├── QuestionId.kt
    │   ├── AnswerId.kt
    │   ├── RetrospectiveStatus.kt
    │   ├── RetrospectivePeriod.kt
    │   ├── QuestionCount.kt
    │   ├── AdditionalNotes.kt
    │   ├── command/
    │   │   └── RetrospectiveCommand.kt
    │   └── query/
    │       └── RetrospectiveQuery.kt
    ├── repository/
    │   ├── RetrospectiveRepository.kt
    │   ├── QuestionRepository.kt
    │   └── AnswerRepository.kt
    └── service/
        └── QuestionGenerationService.kt (인터페이스)
```

**주의사항:**
- `RetrospectiveId`, `TaskId`, `SensitivityLevel`은 common/domain에 위치
- Retrospective 도메인에서는 이들을 import하여 사용
- Task 도메인도 동일하게 common의 VO를 import하여 사용

#### Commands
```kotlin
package xyz.robinjoon.growweek.retrospective.domain.model.command

import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId

sealed interface RetrospectiveCommand {
    data class CreateRetrospective(
        val userId: UserId,
        val period: RetrospectivePeriod,
        val questionCount: QuestionCount = QuestionCount.DEFAULT
    ) : RetrospectiveCommand

    data class GenerateQuestions(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveCommand

    data class WriteAnswer(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId,
        val questionId: QuestionId,
        val content: String?
    ) : RetrospectiveCommand

    data class WriteAdditionalNotes(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId,
        val notes: AdditionalNotes
    ) : RetrospectiveCommand

    data class CompleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveCommand
}
```

#### Queries
```kotlin
sealed interface RetrospectiveQuery {
    data class GetRetrospectiveById(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveQuery

    data class GetRetrospectivesByPeriod(
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : RetrospectiveQuery

    data class GetMonthlyRetrospectives(
        val userId: UserId,
        val year: Int,
        val month: Int
    ) : RetrospectiveQuery

    data class GetRetrospectiveWithQuestionsAndAnswers(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveQuery
}
```

#### Domain Service: QuestionGenerationService
```kotlin
package xyz.robinjoon.growweek.retrospective.domain.service

import xyz.robinjoon.growweek.task.domain.model.Task

interface QuestionGenerationService {
    /**
     * 주간 할일 데이터를 기반으로 회고 질문 생성
     *
     * @param tasks 주간 할일 목록 (민감도에 따라 필터링된 데이터)
     * @param questionCount 생성할 질문 개수
     * @return 생성된 질문 목록
     */
    suspend fun generateQuestions(
        tasks: List<Task>,
        questionCount: QuestionCount
    ): List<String>
}
```

**주의사항:**
- QuestionGenerationService는 Task 도메인 모델을 직접 사용
- Application Layer에서 Task를 조회하고 민감도에 따라 필터링한 후 전달

### 4.2 Application Layer

#### 디렉토리 구조
```
retrospective/
└── application/
    ├── command/
    │   ├── CreateRetrospectiveCommand.kt
    │   ├── GenerateQuestionsCommand.kt
    │   ├── WriteAnswerCommand.kt
    │   ├── WriteAdditionalNotesCommand.kt
    │   └── CompleteRetrospectiveCommand.kt
    ├── query/
    │   ├── GetRetrospectiveQuery.kt
    │   ├── GetRetrospectivesByPeriodQuery.kt
    │   └── GetMonthlyRetrospectivesQuery.kt
    ├── dto/
    │   ├── RetrospectiveDto.kt
    │   ├── QuestionDto.kt
    │   ├── AnswerDto.kt
    │   └── RetrospectiveDetailDto.kt
    ├── usecase/
    │   ├── CreateRetrospectiveUseCase.kt
    │   ├── GenerateQuestionsUseCase.kt
    │   ├── WriteAnswerUseCase.kt
    │   ├── WriteAdditionalNotesUseCase.kt
    │   ├── CompleteRetrospectiveUseCase.kt
    │   ├── GetRetrospectiveUseCase.kt
    │   └── GetMonthlyRetrospectivesUseCase.kt
    └── service/
        ├── RetrospectiveCommandService.kt
        └── RetrospectiveQueryService.kt
```

#### 주요 Use Case 흐름

**GenerateQuestionsUseCase**
1. 회고 조회 및 검증
2. 해당 기간의 할일 목록 조회 (TaskRepository 사용)
3. 민감도(SensitivityLevel)에 따라 할일 데이터 필터링
   ```kotlin
   // common/domain/SensitivityLevel 사용
   val filteredTasks = tasks.map { task ->
       when (task.sensitivityLevel) {
           SensitivityLevel.NONE -> task  // 모든 정보 포함
           SensitivityLevel.TITLE_ONLY -> task.copy(
               description = null,  // 제목만 포함
               // 기타 민감 정보 제외
           )
           SensitivityLevel.HIDDEN -> null  // 제외
       }
   }.filterNotNull()
   ```
4. QuestionGenerationService를 통해 AI 질문 생성
5. 회고에 질문 추가 및 상태 변경
6. Task에 RetrospectiveId 연결 (TaskRepository 사용)

### 4.3 Infrastructure Layer

#### 디렉토리 구조
```
retrospective/
└── infrastructure/
    ├── persistence/
    │   ├── RetrospectiveTable.kt
    │   ├── QuestionTable.kt
    │   ├── AnswerTable.kt
    │   ├── RetrospectiveRepositoryImpl.kt
    │   ├── QuestionRepositoryImpl.kt
    │   └── AnswerRepositoryImpl.kt
    └── external/
        └── OpenAIQuestionGenerationService.kt
```

#### Exposed ORM Table 정의
```kotlin
object RetrospectiveTable : LongIdTable("retrospectives") {
    val userId = long("user_id")
    val startDate = date("start_date")
    val endDate = date("end_date")
    val status = varchar("status", 50)
    val questionCount = integer("question_count")
    val additionalNotes = text("additional_notes").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object QuestionTable : LongIdTable("questions") {
    val retrospectiveId = long("retrospective_id").references(RetrospectiveTable.id)
    val content = text("content")
    val order = integer("order")
    val createdAt = datetime("created_at")
}

object AnswerTable : LongIdTable("answers") {
    val questionId = long("question_id").references(QuestionTable.id)
    val content = text("content").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
```

#### OpenAI Integration
```kotlin
@Service
class OpenAIQuestionGenerationService(
    private val openAIClient: OpenAIClient
) : QuestionGenerationService {

    override suspend fun generateQuestions(
        tasks: List<Task>,
        questionCount: QuestionCount
    ): List<String> {
        val prompt = buildPrompt(tasks, questionCount)
        val response = openAIClient.createCompletion(prompt)
        return parseQuestions(response, questionCount)
    }

    private fun buildPrompt(tasks: List<Task>, questionCount: QuestionCount): String {
        // 할일 데이터를 바탕으로 프롬프트 생성
        // 민감도에 따라 필터링된 데이터 사용
    }
}
```

### 4.4 Presentation Layer

#### 디렉토리 구조
```
retrospective/
└── presentation/
    └── rest/
        ├── controller/
        │   └── RetrospectiveController.kt
        ├── request/
        │   ├── CreateRetrospectiveRequest.kt
        │   ├── GenerateQuestionsRequest.kt
        │   ├── WriteAnswerRequest.kt
        │   ├── WriteAdditionalNotesRequest.kt
        │   └── CompleteRetrospectiveRequest.kt
        └── response/
            ├── RetrospectiveResponse.kt
            ├── QuestionResponse.kt
            ├── AnswerResponse.kt
            └── RetrospectiveDetailResponse.kt
```

#### API 엔드포인트

```kotlin
@RestController
@RequestMapping("/api/v1/retrospectives")
class RetrospectiveController {

    // 회고 생성
    POST /api/v1/retrospectives

    // 질문 생성
    POST /api/v1/retrospectives/{retrospectiveId}/questions/generate

    // 답변 작성/수정
    POST /api/v1/retrospectives/{retrospectiveId}/answers

    // 기타 회고 내용 작성
    POST /api/v1/retrospectives/{retrospectiveId}/notes

    // 회고 완료
    POST /api/v1/retrospectives/{retrospectiveId}/complete

    // 회고 조회
    GET /api/v1/retrospectives/{retrospectiveId}

    // 기간별 회고 목록 조회
    GET /api/v1/retrospectives?startDate={startDate}&endDate={endDate}

    // 월별 회고 목록 조회
    GET /api/v1/retrospectives/monthly?year={year}&month={month}
}
```

## 5. 구현 순서

### Phase 0: Task 도메인 리팩토링 (공유 VO 이동)

회고 기능 구현 전에 먼저 Task 도메인의 공유 Value Objects를 common 패키지로 이동합니다.

#### 5.0.1 이동할 파일
```
server/src/main/kotlin/xyz/robinjoon/growweek/
  task/domain/model/TaskId.kt           → common/domain/TaskId.kt
  task/domain/model/RetrospectiveId.kt  → common/domain/RetrospectiveId.kt
  task/domain/model/SensitivityLevel.kt → common/domain/SensitivityLevel.kt
```

#### 5.0.2 수정 작업
1. **파일 이동**
   - `TaskId.kt`를 `common/domain/`로 이동
   - `RetrospectiveId.kt`를 `common/domain/`로 이동
   - `SensitivityLevel.kt`를 `common/domain/`로 이동

2. **패키지명 변경**
   ```kotlin
   // 변경 전
   package xyz.robinjoon.growweek.task.domain.model

   // 변경 후
   package xyz.robinjoon.growweek.common.domain
   ```

3. **Task 도메인 전체 import 문 수정**
   - `Task.kt`: import 문 수정
   - `TaskCommand.kt`: import 문 수정
   - `TaskQuery.kt`: import 문 수정
   - Application, Infrastructure, Presentation 계층의 모든 파일 import 수정

4. **테스트 코드 import 문 수정**

#### 5.0.3 검증
- 빌드 성공 확인: `./gradlew build`
- 테스트 통과 확인: `./gradlew test`
- Task 관련 API 동작 확인

### Phase 1: Domain Layer 구현
1. Value Objects 구현
   - QuestionId, AnswerId (Retrospective 도메인 전용)
   - RetrospectiveStatus
   - RetrospectivePeriod
   - QuestionCount
   - AdditionalNotes

2. Entity 구현
   - Question
   - Answer

3. Aggregate Root 구현
   - Retrospective

4. Repository 인터페이스 정의
   - RetrospectiveRepository
   - QuestionRepository
   - AnswerRepository

5. Domain Service 인터페이스 정의
   - QuestionGenerationService

6. Command/Query 모델 정의

### Phase 2: Infrastructure Layer 구현
1. Exposed ORM Table 정의
   - RetrospectiveTable
   - QuestionTable
   - AnswerTable

2. Repository 구현
   - RetrospectiveRepositoryImpl
   - QuestionRepositoryImpl
   - AnswerRepositoryImpl

3. QuestionGenerationService 구현
   - OpenAIQuestionGenerationService (또는 임시 Mock 구현)

### Phase 3: Application Layer 구현
1. DTO 정의
2. Use Case 인터페이스 정의
3. Application Service 구현
   - RetrospectiveCommandService
   - RetrospectiveQueryService

### Phase 4: Presentation Layer 구현
1. Request/Response DTO 정의
2. Controller 구현
3. OpenAPI 문서화

### Phase 5: 통합 및 테스트
1. 단위 테스트 (Kotest)
2. 통합 테스트
3. API 테스트

## 6. 기술적 고려사항

### 6.1 공유 Value Objects (Shared Kernel)
- TaskId, RetrospectiveId, SensitivityLevel을 common 패키지에 배치
- 두 도메인 간 순환 참조 방지
- 공유 VO 수정 시 양쪽 도메인에 미치는 영향 고려
- 공유 VO는 불변성과 안정성을 최우선으로 유지

### 6.2 AI 질문 생성
- OpenAI API 또는 다른 LLM API 사용
- 비동기 처리 (suspend 함수)
- 타임아웃 설정
- 실패 시 재시도 로직

### 6.3 민감도 필터링
- Task 조회 후 `common.domain.SensitivityLevel`에 따라 데이터 필터링
- Application Layer에서 필터링 수행
- QuestionGenerationService에 필터링된 데이터만 전달
- 필터링 로직:
  ```kotlin
  when (task.sensitivityLevel) {
      SensitivityLevel.NONE -> 전체 정보 포함
      SensitivityLevel.TITLE_ONLY -> 제목만 포함, 나머지 null 처리
      SensitivityLevel.HIDDEN -> 제외
  }
  ```

### 6.4 트랜잭션 관리
- 질문 생성 및 Task 연결은 하나의 트랜잭션으로 처리
- 답변 작성은 개별 트랜잭션
- Task 도메인과 Retrospective 도메인 간 트랜잭션 경계 명확히 설정

### 6.5 성능 최적화
- 질문/답변 조회 시 JOIN 최적화
- Redis 캐싱 고려 (월별 회고 목록 등)

### 6.6 작성 기간 검증
- 매 주 월요일 0시 0분까지 작성 가능 검증
- 스케줄러를 통한 자동 상태 변경 고려 (선택적)

## 7. 데이터베이스 스키마

```sql
-- 회고 테이블
CREATE TABLE retrospectives (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    question_count INTEGER NOT NULL,
    additional_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 질문 테이블
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    retrospective_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_retrospective FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id)
);

-- 답변 테이블
CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

-- 인덱스
CREATE INDEX idx_retrospectives_user_id ON retrospectives(user_id);
CREATE INDEX idx_retrospectives_period ON retrospectives(start_date, end_date);
CREATE INDEX idx_questions_retrospective_id ON questions(retrospective_id);
CREATE INDEX idx_answers_question_id ON answers(question_id);
```

## 8. 테스트 전략

### 8.1 단위 테스트
- Domain Model 비즈니스 로직 테스트
- Value Object 유효성 검증 테스트
- Repository 테스트 (H2 in-memory DB)

### 8.2 통합 테스트
- Use Case 전체 플로우 테스트
- API 엔드포인트 테스트

### 8.3 테스트 커버리지
- 최소 70% 이상 (JaCoCo)

## 9. 향후 확장 가능성

1. **기간 설정 기능**: 주 시작일을 사용자가 설정 가능
2. **템플릿 질문**: AI 생성 외에 미리 정의된 질문 템플릿 제공
3. **회고 공유**: 팀원들과 회고 공유 기능
4. **통계 및 분석**: 월별/분기별 회고 통계 제공
5. **알림**: 회고 작성 기한 알림
6. **추가 도메인 연계**: Report, Analytics 등 다른 도메인에서도 공유 VO 활용 가능

## 10. 요약

이 구현 계획은 DDD + Clean Architecture + CQRS 패턴을 따르며, 특히 **Shared Kernel 패턴**을 통해 Task와 Retrospective 도메인 간의 관계를 깔끔하게 설계합니다.

**핵심 포인트:**
- Phase 0에서 먼저 Task 도메인 리팩토링 (공유 VO 이동)
- 순환 참조 없는 명확한 도메인 간 의존성
- AI 기반 회고 질문 자동 생성
- 민감도에 따른 데이터 필터링
- CQRS를 통한 읽기/쓰기 최적화
