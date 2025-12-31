# 회원(Member) 기능 구현 작업 계획

## 1. 개요

GrowWeek 프로젝트에 회원 가입, 로그인, 인증 기능을 구현합니다. 본 문서는 DDD, Clean Architecture, CQRS 패턴을 적용한 구현 계획을 제시합니다.

### 참고 문서
- [전체 플로우](https://www.notion.so/robinjoon/2cb26f51b6c080f298c8cb701bdbe7de)

### 목표

전체 플로우 시나리오를 수행하기 위해서는 **사용자 식별**이 필수입니다:
1. 매주 할일 작성 → 누가 작성했는지 식별 필요
2. 매일 할일 관리 → 본인의 할일만 조회/수정 가능
3. 매주 회고 작성 → 사용자별 회고 관리
4. 매월 회고 조회 → 본인의 회고만 조회 가능

## 2. 비즈니스 요구사항

### 2.1 MVP 기능 범위

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 회원가입 | 이메일/비밀번호 기반 회원가입 | 필수 |
| 로그인 | JWT 토큰 기반 인증 | 필수 |
| 현재 사용자 조회 | 로그인된 사용자 정보 조회 | 필수 |
| 로그아웃 | Access Token 무효화 | 선택 |

### 2.2 고도화 기능 (향후 구현)

| 기능 | 설명 |
|------|------|
| OAuth2 소셜 로그인 | 카카오, 구글 등 소셜 로그인 |
| 이메일 인증 | 회원가입 시 이메일 인증 |
| 비밀번호 재설정 | 이메일 기반 비밀번호 재설정 |
| 프로필 관리 | 프로필 이미지, 닉네임 변경 |
| Refresh Token | Access Token 갱신 |

### 2.3 회원 데이터 구조

| 필드 | 타입 | 제약사항 | 설명 |
|------|------|----------|------|
| id | Long | PK, Auto | 회원 ID |
| email | String | 100자 이하, Unique, Not Null | 이메일 (로그인 ID) |
| password | String | Not Null | 암호화된 비밀번호 |
| nickname | String | 50자 이하, Not Null | 닉네임 |
| status | Enum | Not Null, Default: ACTIVE | 회원 상태 |
| createdAt | Timestamp | Not Null | 가입 시각 |
| updatedAt | Timestamp | Not Null | 수정 시각 |

### 2.4 회원 상태 (MemberStatus)

```kotlin
enum class MemberStatus {
    ACTIVE,     // 활성
    INACTIVE,   // 비활성 (탈퇴)
    SUSPENDED   // 정지
}
```

### 2.5 인증 방식

- **JWT (JSON Web Token)** 기반 인증
- Access Token 유효 기간: 1시간
- Refresh Token 유효 기간: 7일 (고도화 시 구현)
- Token 저장: 클라이언트 측 (LocalStorage 또는 HttpOnly Cookie)

## 3. 도메인 모델 설계

### 3.1 Bounded Context

- **Context Name**: `member`
- **Package**: `xyz.robinjoon.growweek.member`

### 3.2 디렉토리 구조

```
member/
├── presentation/
│   └── rest/
│       ├── request/
│       │   ├── SignUpRequest.kt
│       │   └── LoginRequest.kt
│       ├── response/
│       │   ├── MemberResponse.kt
│       │   └── TokenResponse.kt
│       └── controller/
│           └── MemberController.kt
├── application/
│   ├── command/
│   │   └── MemberApplicationCommand.kt
│   ├── dto/
│   │   ├── MemberDto.kt
│   │   └── TokenDto.kt
│   ├── query/
│   │   └── MemberApplicationQuery.kt
│   ├── service/
│   │   ├── SignUpService.kt
│   │   ├── LoginService.kt
│   │   └── GetMemberService.kt
│   └── usecase/
│       ├── SignUpUseCase.kt
│       ├── LoginUseCase.kt
│       └── GetMemberUseCase.kt
├── domain/
│   ├── model/
│   │   ├── Member.kt
│   │   ├── Email.kt
│   │   ├── Password.kt
│   │   ├── Nickname.kt
│   │   ├── MemberStatus.kt
│   │   ├── command/
│   │   │   └── MemberCommand.kt
│   │   └── query/
│   │       └── MemberQuery.kt
│   ├── repository/
│   │   └── MemberRepository.kt
│   └── service/
│       └── PasswordEncoder.kt
└── infrastructure/
    ├── persistence/
    │   ├── MemberTable.kt
    │   └── ExposedMemberRepository.kt
    └── security/
        ├── JwtTokenProvider.kt
        ├── JwtAuthenticationFilter.kt
        └── BCryptPasswordEncoder.kt
```

### 3.3 Aggregate Root

#### Member (회원)

```kotlin
// domain/model/Member.kt
data class Member(
    val id: MemberId,
    val email: Email,
    val password: Password,
    val nickname: Nickname,
    val status: MemberStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun create(
            email: Email,
            password: Password,
            nickname: Nickname
        ): Member {
            val now = LocalDateTime.now()
            return Member(
                id = MemberId(0), // Auto-generated
                email = email,
                password = password,
                nickname = nickname,
                status = MemberStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    fun isActive(): Boolean = status == MemberStatus.ACTIVE

    fun deactivate(): Member {
        return copy(
            status = MemberStatus.INACTIVE,
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateNickname(newNickname: Nickname): Member {
        return copy(
            nickname = newNickname,
            updatedAt = LocalDateTime.now()
        )
    }
}
```

### 3.4 Value Objects

```kotlin
// domain/model/Email.kt
@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "이메일은 비어있을 수 없습니다" }
        require(value.length <= 100) { "이메일은 100자 이하여야 합니다" }
        require(EMAIL_REGEX.matches(value)) { "올바른 이메일 형식이 아닙니다" }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}

// domain/model/Password.kt
@JvmInline
value class Password(val value: String) {
    init {
        require(value.isNotBlank()) { "비밀번호는 비어있을 수 없습니다" }
    }

    companion object {
        fun validate(rawPassword: String) {
            require(rawPassword.length >= 8) { "비밀번호는 8자 이상이어야 합니다" }
            require(rawPassword.length <= 100) { "비밀번호는 100자 이하여야 합니다" }
        }
    }
}

// domain/model/Nickname.kt
@JvmInline
value class Nickname(val value: String) {
    init {
        require(value.isNotBlank()) { "닉네임은 비어있을 수 없습니다" }
        require(value.length <= 50) { "닉네임은 50자 이하여야 합니다" }
    }
}

// domain/model/MemberStatus.kt
enum class MemberStatus {
    ACTIVE,     // 활성
    INACTIVE,   // 비활성 (탈퇴)
    SUSPENDED   // 정지
}
```

### 3.5 MemberId (common에 위치)

현재 프로젝트에는 `UserId`가 `common/domain`에 이미 정의되어 있습니다. 이를 활용하거나 `MemberId`로 마이그레이션이 필요합니다.

```kotlin
// common/domain/MemberId.kt (기존 UserId를 대체 또는 병행)
@JvmInline
value class MemberId(val value: Long) {
    init {
        require(value >= 0) { "MemberId must be greater than or equal to 0" }
    }
}
```

**결정 사항**: 기존 `UserId`를 `MemberId`로 변경하고, 관련 코드를 마이그레이션합니다.

### 3.6 Domain Command & Query

```kotlin
// domain/model/command/MemberCommand.kt
sealed interface MemberCommand {
    data class CreateMember(
        val email: Email,
        val password: Password,
        val nickname: Nickname
    ) : MemberCommand

    data class UpdateMember(
        val memberId: MemberId,
        val nickname: Nickname?
    ) : MemberCommand

    data class DeactivateMember(
        val memberId: MemberId
    ) : MemberCommand
}

// domain/model/query/MemberQuery.kt
sealed class MemberQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    data class ById(
        val memberId: MemberId,
        override val pageInfo: PageInfo = SinglePageInfo
    ) : MemberQuery(pageInfo)

    data class ByEmail(
        val email: Email,
        override val pageInfo: PageInfo = SinglePageInfo
    ) : MemberQuery(pageInfo)

    companion object {
        object SinglePageInfo : PageInfo {
            override val size: Int = 1
        }
    }
}
```

### 3.7 Repository Interface

```kotlin
// domain/repository/MemberRepository.kt
interface MemberRepository {
    fun saveAll(commands: List<MemberCommand>): List<Member>
    fun findAll(query: MemberQuery): Page<Member>

    // 편의 메서드 (기본 구현)
    fun save(command: MemberCommand): Member {
        return saveAll(listOf(command)).first()
    }

    fun findById(memberId: MemberId): Member? {
        return findAll(MemberQuery.ById(memberId)).content.firstOrNull()
    }

    fun findByEmail(email: Email): Member? {
        return findAll(MemberQuery.ByEmail(email)).content.firstOrNull()
    }

    fun existsByEmail(email: Email): Boolean {
        return findByEmail(email) != null
    }
}
```

### 3.8 Domain Service

```kotlin
// domain/service/PasswordEncoder.kt
interface PasswordEncoder {
    fun encode(rawPassword: String): String
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
```

## 4. 계층별 구현 계획

### 4.1 Application Layer

#### 4.1.1 Commands

```kotlin
// application/command/MemberApplicationCommand.kt
sealed interface MemberApplicationCommand {
    data class SignUp(
        val email: String,
        val password: String,
        val nickname: String
    ) : MemberApplicationCommand

    data class Login(
        val email: String,
        val password: String
    ) : MemberApplicationCommand
}
```

#### 4.1.2 DTOs

```kotlin
// application/dto/MemberDto.kt
data class MemberDto(
    val id: Long,
    val email: String,
    val nickname: String,
    val status: String,
    val createdAt: LocalDateTime
)

// application/dto/TokenDto.kt
data class TokenDto(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long // seconds
)
```

#### 4.1.3 Use Cases

```kotlin
// application/usecase/SignUpUseCase.kt
interface SignUpUseCase {
    fun execute(command: MemberApplicationCommand.SignUp): MemberDto
}

// application/usecase/LoginUseCase.kt
interface LoginUseCase {
    fun execute(command: MemberApplicationCommand.Login): TokenDto
}

// application/usecase/GetMemberUseCase.kt
interface GetMemberUseCase {
    fun execute(memberId: MemberId): MemberDto?
}
```

#### 4.1.4 Service Implementations

```kotlin
// application/service/SignUpService.kt
@Service
class SignUpService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) : SignUpUseCase {

    @Transactional
    override fun execute(command: MemberApplicationCommand.SignUp): MemberDto {
        val email = Email(command.email)

        // 이메일 중복 검사
        if (memberRepository.existsByEmail(email)) {
            throw DuplicateEmailException(command.email)
        }

        // 비밀번호 유효성 검사
        Password.validate(command.password)

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(command.password)

        // 회원 생성
        val createCommand = MemberCommand.CreateMember(
            email = email,
            password = Password(encodedPassword),
            nickname = Nickname(command.nickname)
        )

        val member = memberRepository.save(createCommand)
        return member.toDto()
    }
}

// application/service/LoginService.kt
@Service
class LoginService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) : LoginUseCase {

    @Transactional(readOnly = true)
    override fun execute(command: MemberApplicationCommand.Login): TokenDto {
        val email = Email(command.email)

        val member = memberRepository.findByEmail(email)
            ?: throw InvalidCredentialsException()

        if (!member.isActive()) {
            throw MemberNotActiveException()
        }

        if (!passwordEncoder.matches(command.password, member.password.value)) {
            throw InvalidCredentialsException()
        }

        val accessToken = jwtTokenProvider.createToken(member.id)

        return TokenDto(
            accessToken = accessToken,
            expiresIn = jwtTokenProvider.getExpirationInSeconds()
        )
    }
}

// application/service/GetMemberService.kt
@Service
class GetMemberService(
    private val memberRepository: MemberRepository
) : GetMemberUseCase {

    @Transactional(readOnly = true)
    override fun execute(memberId: MemberId): MemberDto? {
        return memberRepository.findById(memberId)?.toDto()
    }
}
```

### 4.2 Infrastructure Layer

#### 4.2.1 Exposed Table Definition

```kotlin
// infrastructure/persistence/MemberTable.kt
object MemberTable : LongIdTable("members") {
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 200)
    val nickname = varchar("nickname", 50)
    val status = varchar("status", 20).default(MemberStatus.ACTIVE.name)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index(false, status)
    }
}
```

#### 4.2.2 Repository Implementation

```kotlin
// infrastructure/persistence/ExposedMemberRepository.kt
@Repository
class ExposedMemberRepository : MemberRepository {

    @Transactional
    override fun saveAll(commands: List<MemberCommand>): List<Member> {
        return commands.map { command ->
            when (command) {
                is MemberCommand.CreateMember -> createMember(command)
                is MemberCommand.UpdateMember -> updateMember(command)
                is MemberCommand.DeactivateMember -> deactivateMember(command)
            }
        }
    }

    private fun createMember(command: MemberCommand.CreateMember): Member {
        val now = LocalDateTime.now()
        val id = MemberTable.insertAndGetId {
            it[email] = command.email.value
            it[password] = command.password.value
            it[nickname] = command.nickname.value
            it[status] = MemberStatus.ACTIVE.name
            it[createdAt] = now
            it[updatedAt] = now
        }.value

        return Member(
            id = MemberId(id),
            email = command.email,
            password = command.password,
            nickname = command.nickname,
            status = MemberStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun updateMember(command: MemberCommand.UpdateMember): Member {
        val now = LocalDateTime.now()
        MemberTable.update({ MemberTable.id eq command.memberId.value }) {
            command.nickname?.let { nickname ->
                it[MemberTable.nickname] = nickname.value
            }
            it[updatedAt] = now
        }

        return findById(command.memberId)!!
    }

    private fun deactivateMember(command: MemberCommand.DeactivateMember): Member {
        val now = LocalDateTime.now()
        MemberTable.update({ MemberTable.id eq command.memberId.value }) {
            it[status] = MemberStatus.INACTIVE.name
            it[updatedAt] = now
        }

        return findById(command.memberId)!!
    }

    @Transactional(readOnly = true)
    override fun findAll(query: MemberQuery): Page<Member> {
        return when (query) {
            is MemberQuery.ById -> findById(query)
            is MemberQuery.ByEmail -> findByEmail(query)
        }
    }

    private fun findById(query: MemberQuery.ById): Page<Member> {
        val member = MemberTable.select { MemberTable.id eq query.memberId.value }
            .map { it.toMember() }
            .singleOrNull()

        return SingleItemPage(member)
    }

    private fun findByEmail(query: MemberQuery.ByEmail): Page<Member> {
        val member = MemberTable.select { MemberTable.email eq query.email.value }
            .map { it.toMember() }
            .singleOrNull()

        return SingleItemPage(member)
    }

    private fun ResultRow.toMember(): Member {
        return Member(
            id = MemberId(this[MemberTable.id].value),
            email = Email(this[MemberTable.email]),
            password = Password(this[MemberTable.password]),
            nickname = Nickname(this[MemberTable.nickname]),
            status = MemberStatus.valueOf(this[MemberTable.status]),
            createdAt = this[MemberTable.createdAt],
            updatedAt = this[MemberTable.updatedAt]
        )
    }
}

// Helper class
class SingleItemPage<T>(private val item: T?) : Page<T> {
    override val content: List<T> = listOfNotNull(item)
    override val totalElements: Long = if (item != null) 1L else 0L
    override val totalPages: Int = if (item != null) 1 else 0
    override val pageNumber: Int = 0
    override val pageSize: Int = 1
    override val hasNext: Boolean = false
    override val hasPrevious: Boolean = false
}
```

#### 4.2.3 Security Components

```kotlin
// infrastructure/security/BCryptPasswordEncoderImpl.kt
@Component
class BCryptPasswordEncoderImpl : PasswordEncoder {
    private val encoder = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): String {
        return encoder.encode(rawPassword)
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return encoder.matches(rawPassword, encodedPassword)
    }
}

// infrastructure/security/JwtTokenProvider.kt
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expirationMs: Long
) {
    private val secretKey: Key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun createToken(memberId: MemberId): String {
        val now = Date()
        val expiration = Date(now.time + expirationMs)

        return Jwts.builder()
            .setSubject(memberId.value.toString())
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getMemberIdFromToken(token: String): MemberId {
        val claims = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body

        return MemberId(claims.subject.toLong())
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getExpirationInSeconds(): Long = expirationMs / 1000
}

// infrastructure/security/JwtAuthenticationFilter.kt
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val memberId = jwtTokenProvider.getMemberIdFromToken(token)
            val authentication = JwtAuthenticationToken(memberId)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        return if (header?.startsWith("Bearer ") == true) {
            header.substring(7)
        } else {
            null
        }
    }
}

// infrastructure/security/JwtAuthenticationToken.kt
class JwtAuthenticationToken(
    val memberId: MemberId
) : AbstractAuthenticationToken(emptyList()) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): MemberId = memberId
}
```

### 4.3 Presentation Layer

#### 4.3.1 Request DTOs

```kotlin
// presentation/rest/request/SignUpRequest.kt
data class SignUpRequest(
    val email: String,
    val password: String,
    val nickname: String
)

// presentation/rest/request/LoginRequest.kt
data class LoginRequest(
    val email: String,
    val password: String
)
```

#### 4.3.2 Response DTOs

```kotlin
// presentation/rest/response/MemberResponse.kt
data class MemberResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val createdAt: String
)

// presentation/rest/response/TokenResponse.kt
data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)
```

#### 4.3.3 Controller

```kotlin
// presentation/rest/controller/MemberController.kt
@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "회원 API")
class MemberController(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val getMemberUseCase: GetMemberUseCase
) {

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<MemberResponse> {
        val command = MemberApplicationCommand.SignUp(
            email = request.email,
            password = request.password,
            nickname = request.nickname
        )
        val member = signUpUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(member.toResponse())
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val command = MemberApplicationCommand.Login(
            email = request.email,
            password = request.password
        )
        val token = loginUseCase.execute(command)
        return ResponseEntity.ok(token.toResponse())
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 조회", description = "로그인된 사용자의 정보를 조회합니다")
    fun getCurrentMember(authentication: Authentication): ResponseEntity<MemberResponse> {
        val memberId = (authentication as JwtAuthenticationToken).memberId
        val member = getMemberUseCase.execute(memberId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(member.toResponse())
    }
}
```

### 4.4 Security Configuration 수정

```kotlin
// common/config/SecurityConfig.kt
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    // Swagger UI 및 OpenAPI 문서 경로 허용
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll()
                    // 인증 없이 접근 가능한 API
                    .requestMatchers(
                        "/api/v1/members/signup",
                        "/api/v1/members/login"
                    ).permitAll()
                    // 나머지 API는 인증 필요
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
```

## 5. 데이터베이스 스키마

### 5.1 DDL

```sql
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_members_email ON members(email);
CREATE INDEX idx_members_status ON members(status);
```

## 6. API 명세

### 6.1 회원가입

```
POST /api/v1/members/signup
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "createdAt": "2025-12-28T10:00:00"
}
```

**Error Responses**:
- 400 Bad Request: 유효하지 않은 입력값
- 409 Conflict: 이메일 중복

### 6.2 로그인

```
POST /api/v1/members/login
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Responses**:
- 401 Unauthorized: 잘못된 이메일 또는 비밀번호

### 6.3 현재 사용자 조회

```
GET /api/v1/members/me
Authorization: Bearer {accessToken}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "createdAt": "2025-12-28T10:00:00"
}
```

**Error Responses**:
- 401 Unauthorized: 인증 토큰 없음 또는 만료
- 404 Not Found: 사용자 없음

## 7. 예외 처리

### 7.1 커스텀 예외

```kotlin
// common/exception/MemberException.kt
sealed class MemberException(message: String) : RuntimeException(message)

class DuplicateEmailException(email: String) :
    MemberException("이미 사용 중인 이메일입니다: $email")

class InvalidCredentialsException :
    MemberException("이메일 또는 비밀번호가 올바르지 않습니다")

class MemberNotFoundException(memberId: Long) :
    MemberException("회원을 찾을 수 없습니다: $memberId")

class MemberNotActiveException :
    MemberException("비활성화된 계정입니다")

class InvalidTokenException :
    MemberException("유효하지 않은 토큰입니다")
```

### 7.2 예외 핸들러

```kotlin
// common/exception/GlobalExceptionHandler.kt (추가)
@ExceptionHandler(DuplicateEmailException::class)
fun handleDuplicateEmail(ex: DuplicateEmailException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ErrorResponse("DUPLICATE_EMAIL", ex.message))
}

@ExceptionHandler(InvalidCredentialsException::class)
fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse("INVALID_CREDENTIALS", ex.message))
}

@ExceptionHandler(MemberNotFoundException::class)
fun handleMemberNotFound(ex: MemberNotFoundException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse("MEMBER_NOT_FOUND", ex.message))
}
```

## 8. 설정 파일

### 8.1 application.yml 추가 설정

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-for-development-only}
  expiration: 3600000 # 1시간 (밀리초)
```

### 8.2 build.gradle.kts 의존성 추가

```kotlin
dependencies {
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // BCrypt (Spring Security에 포함)
    implementation("org.springframework.boot:spring-boot-starter-security")
}
```

## 9. 기존 코드 마이그레이션

### 9.1 UserId → MemberId 마이그레이션

기존 `UserId`를 `MemberId`로 변경하고 관련 코드를 수정합니다:

1. `common/domain/UserId.kt` → `common/domain/MemberId.kt` 이름 변경
2. Task, Retrospective 등에서 UserId 사용처를 MemberId로 변경
3. Controller에서 `@AuthenticationPrincipal` 어노테이션 제거 후 `Authentication` 객체에서 MemberId 추출

### 9.2 Controller 수정 예시

```kotlin
// task/presentation/rest/controller/TaskController.kt (수정)
@PostMapping
fun createTask(
    @RequestBody request: CreateTaskRequest,
    authentication: Authentication
): ResponseEntity<TaskResponse> {
    val memberId = (authentication as JwtAuthenticationToken).memberId
    val command = request.toCommand(memberId.value)
    val response = createTaskUseCase.execute(command)
    return ResponseEntity.status(HttpStatus.CREATED).body(response.toResponse())
}
```

## 10. 테스트 계획

### 10.1 단위 테스트

- **Domain Layer**:
  - Value Object 검증 로직 테스트 (Email, Password, Nickname)
  - Member Entity 상태 전환 테스트

- **Application Layer**:
  - SignUpService 테스트 (이메일 중복, 비밀번호 암호화)
  - LoginService 테스트 (인증 성공/실패)

### 10.2 테스트 코드 예시

```kotlin
// domain/model/EmailTest.kt
class EmailTest : BehaviorSpec({
    Given("이메일 Value Object 생성 시") {
        When("유효한 이메일 형식이면") {
            Then("정상적으로 생성된다") {
                val email = Email("test@example.com")
                email.value shouldBe "test@example.com"
            }
        }

        When("비어있는 이메일이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("")
                }
            }
        }

        When("유효하지 않은 이메일 형식이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("invalid-email")
                }
            }
        }
    }
})

// application/service/SignUpServiceTest.kt
class SignUpServiceTest : BehaviorSpec({
    val memberRepository = mockk<MemberRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val service = SignUpService(memberRepository, passwordEncoder)

    Given("회원가입 요청 시") {
        val command = MemberApplicationCommand.SignUp(
            email = "test@example.com",
            password = "password123",
            nickname = "테스터"
        )

        When("이메일이 중복되지 않으면") {
            every { memberRepository.existsByEmail(any()) } returns false
            every { passwordEncoder.encode(any()) } returns "encoded_password"
            every { memberRepository.save(any()) } returns mockMember

            Then("회원이 생성된다") {
                val result = service.execute(command)
                result.email shouldBe "test@example.com"
            }
        }

        When("이메일이 중복되면") {
            every { memberRepository.existsByEmail(any()) } returns true

            Then("DuplicateEmailException이 발생한다") {
                shouldThrow<DuplicateEmailException> {
                    service.execute(command)
                }
            }
        }
    }
})
```

## 11. 구현 단계

### Phase 1: 기본 인프라 구축
1. JWT 관련 의존성 추가
2. JwtTokenProvider 구현
3. JwtAuthenticationFilter 구현
4. BCryptPasswordEncoderImpl 구현
5. SecurityConfig 수정

### Phase 2: Domain & Infrastructure Layer
1. MemberId 마이그레이션 (UserId → MemberId)
2. Member Entity 및 Value Objects 구현
3. MemberRepository 인터페이스 정의
4. MemberTable (Exposed) 정의
5. ExposedMemberRepository 구현

### Phase 3: Application Layer
1. MemberApplicationCommand 정의
2. MemberDto 정의
3. SignUpUseCase/Service 구현
4. LoginUseCase/Service 구현
5. GetMemberUseCase/Service 구현

### Phase 4: Presentation Layer
1. Request/Response DTOs 정의
2. MemberController 구현
3. 예외 핸들러 추가

### Phase 5: 기존 코드 수정
1. TaskController 수정 (Authentication 객체 활용)
2. RetrospectiveController 수정
3. 관련 테스트 코드 수정

### Phase 6: 테스트 작성
1. Domain Layer 단위 테스트
2. Application Layer 단위 테스트
3. 통합 테스트 (선택)

## 12. 보안 고려사항

1. **비밀번호 정책**: 최소 8자 이상, 영문/숫자 조합 권장
2. **비밀번호 암호화**: BCrypt 사용 (Salt 자동 생성)
3. **JWT Secret**: 환경 변수로 관리, 256bit 이상 권장
4. **Token 저장**: HttpOnly Cookie 권장 (XSS 방지)
5. **HTTPS**: 프로덕션 환경에서 필수
6. **Rate Limiting**: 로그인 시도 횟수 제한 (고도화)

## 13. 고도화 계획

### 13.1 Refresh Token 도입
- Access Token: 15분
- Refresh Token: 7일
- Token 갱신 API 추가

### 13.2 OAuth2 소셜 로그인
- 카카오, 구글, 네이버 로그인
- 소셜 로그인 연동 테이블 추가

### 13.3 이메일 인증
- 회원가입 시 이메일 인증 링크 발송
- 인증 완료 전까지 PENDING 상태 유지

---

**작성일**: 2025-12-28
**작성자**: Claude Code
**버전**: 1.0
