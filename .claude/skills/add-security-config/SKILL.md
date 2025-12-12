---
name: add-security-config
description: Spring Security 7.x와 JWT를 사용하여 인증/인가를 구현합니다. 보안 설정, JWT 토큰 처리, 엔드포인트 권한 설정이 필요할 때 사용하세요.
---

# Add Security Config

## Instructions

### 1. Security Configuration

**위치**: `common/security/config/`

**핵심 구성**:
- SecurityFilterChain 설정
- JWT Authentication Filter 추가
- CORS 설정
- CSRF 설정 (API는 보통 비활성화)

### 2. JWT Provider

**역할**:
- JWT 토큰 생성
- JWT 토큰 검증
- Claims 추출

**라이브러리**: JJWT 0.12.6

### 3. Authentication Filter

**역할**:
- Request에서 JWT 토큰 파싱
- 토큰 검증
- SecurityContext에 인증 정보 설정

### 4. 예외 처리

- **AuthenticationEntryPoint**: 인증 실패 (401)
- **AccessDeniedHandler**: 인가 실패 (403)

### 5. 위치

`common/security/`

## Examples

### Security Configuration

```kotlin
// common/security/config/SecurityConfig.kt
package xyz.robinjoon.growweek.common.security.config

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val accessDeniedHandler: JwtAccessDeniedHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    // Public endpoints
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/public/**").permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health"
                    ).permitAll()

                    // Protected endpoints
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000", "https://app.example.com")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            allowCredentials = true
            maxAge = 3600
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
```

### JWT Provider

```kotlin
// common/security/jwt/JwtProvider.kt
package xyz.robinjoon.growweek.common.security.jwt

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(userId: String, roles: List<String>): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun getUserIdFromToken(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.subject
    }

    fun getRolesFromToken(token: String): List<String> {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        @Suppress("UNCHECKED_CAST")
        return claims["roles"] as? List<String> ?: emptyList()
    }
}
```

### JWT Authentication Filter

```kotlin
// common/security/filter/JwtAuthenticationFilter.kt
package xyz.robinjoon.growweek.common.security.filter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)

            if (token != null && jwtProvider.validateToken(token)) {
                val userId = jwtProvider.getUserIdFromToken(token)
                val userDetails = userDetailsService.loadUserByUsername(userId)

                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )

                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            logger.error("Could not set user authentication in security context", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
```

### Authentication Entry Point

```kotlin
// common/security/handler/JwtAuthenticationEntryPoint.kt
package xyz.robinjoon.growweek.common.security.handler

@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {

    private val objectMapper = ObjectMapper()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val errorResponse = ErrorResponse(
            status = 401,
            error = "Unauthorized",
            message = "Authentication required",
            path = request.requestURI
        )

        objectMapper.writeValue(response.outputStream, errorResponse)
    }
}
```

### Access Denied Handler

```kotlin
// common/security/handler/JwtAccessDeniedHandler.kt
package xyz.robinjoon.growweek.common.security.handler

@Component
class JwtAccessDeniedHandler : AccessDeniedHandler {

    private val objectMapper = ObjectMapper()

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val errorResponse = ErrorResponse(
            status = 403,
            error = "Forbidden",
            message = "Access denied",
            path = request.requestURI
        )

        objectMapper.writeValue(response.outputStream, errorResponse)
    }
}
```

### UserDetailsService 구현

```kotlin
// user/infrastructure/security/CustomUserDetailsService.kt
package xyz.robinjoon.growweek.user.infrastructure.security

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(Email(username))
            ?: throw UsernameNotFoundException("User not found: $username")

        return User.builder()
            .username(user.email.value)
            .password(user.password.value)
            .authorities(user.roles.map { SimpleGrantedAuthority("ROLE_$it") })
            .build()
    }
}
```

### 로그인 API 예시

```kotlin
// auth/presentation/AuthController.kt
package xyz.robinjoon.growweek.auth.presentation

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtProvider: JwtProvider
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        val user = authService.authenticate(
            request.email,
            request.password
        )

        val token = jwtProvider.generateToken(
            userId = user.id.value.toString(),
            roles = user.roles
        )

        return LoginResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = 3600
        )
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse {
        val user = authService.register(request.toCommand())
        return UserResponse.from(user)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: UserDetails): UserResponse {
        val user = authService.getUserByEmail(userDetails.username)
        return UserResponse.from(user)
    }
}

data class LoginRequest(
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)
```

### Method Level Security

```kotlin
// Controller 메서드에 권한 설정
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
fun deleteUser(@PathVariable id: Long) {
    // Admin만 접근 가능
}

@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@GetMapping("/reports")
fun getReports() {
    // Admin 또는 Manager만 접근 가능
}

@PreAuthorize("#userId == authentication.principal.username")
@GetMapping("/users/{userId}/profile")
fun getProfile(@PathVariable userId: String) {
    // 본인만 접근 가능
}
```

### application.yml

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-min-256-bits-long-for-hs256-algorithm}
  expiration: 3600000  # 1 hour in milliseconds

spring:
  security:
    user:
      name: admin
      password: admin123
```
