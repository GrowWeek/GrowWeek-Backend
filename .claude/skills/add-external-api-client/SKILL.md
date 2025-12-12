---
name: add-external-api-client
description: Spring Cloud OpenFeign을 사용하여 외부 API 클라이언트를 추가합니다. 외부 시스템과 HTTP 통신이 필요할 때 사용하세요.
---

# Add External API Client

## Instructions

### 1. OpenFeign 활성화

Main Application 클래스에 `@EnableFeignClients` 추가:

```kotlin
@SpringBootApplication
@EnableFeignClients
class ServerApplication
```

### 2. Feign Client 인터페이스

**위치**: `{bounded-context}/infrastructure/external/`

**네이밍**: `{서비스명}Client`

```kotlin
@FeignClient(
    name = "external-service",
    url = "\${external.api.url}",
    configuration = [FeignClientConfig::class]
)
interface ExternalServiceClient {
    @GetMapping("/api/resource/{id}")
    fun getResource(@PathVariable id: String): ResourceDto
}
```

### 3. Request/Response DTOs

외부 API 스펙에 맞는 DTO 정의:

```kotlin
data class ExternalRequestDto(
    val field1: String,
    val field2: Int
)

data class ExternalResponseDto(
    val id: String,
    val data: String
)
```

### 4. Feign Configuration

**로깅**, **타임아웃**, **인코더/디코더** 설정:

```kotlin
@Configuration
class FeignClientConfig {
    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.FULL

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            template.header("Authorization", "Bearer token")
        }
    }
}
```

### 5. Error Decoder

외부 API 에러를 도메인 예외로 변환:

```kotlin
class CustomErrorDecoder : ErrorDecoder {
    override fun decode(
        methodKey: String,
        response: Response
    ): Exception {
        return when (response.status()) {
            404 -> ResourceNotFoundException()
            500 -> ExternalServiceException()
            else -> FeignException.errorStatus(methodKey, response)
        }
    }
}
```

### 6. 도메인 레이어에서 사용

어댑터 패턴으로 도메인 인터페이스 구현:

```kotlin
// domain 인터페이스
interface ExternalService {
    fun fetchData(id: String): DomainData
}

// infrastructure 구현
@Component
class ExternalServiceAdapter(
    private val client: ExternalServiceClient
) : ExternalService {
    override fun fetchData(id: String): DomainData {
        val response = client.getResource(id)
        return response.toDomain()
    }
}
```

### 7. 위치

`{bounded-context}/infrastructure/external/`

## Examples

### 완전한 Feign Client 예시

```kotlin
// infrastructure/external/client/GithubClient.kt
package xyz.robinjoon.growweek.integration.infrastructure.external.client

@FeignClient(
    name = "github",
    url = "\${github.api.url}",
    configuration = [GithubClientConfig::class]
)
interface GithubClient {

    @GetMapping("/users/{username}")
    fun getUser(
        @PathVariable username: String
    ): GithubUserDto

    @GetMapping("/users/{username}/repos")
    fun getRepositories(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") per_page: Int
    ): List<GithubRepoDto>

    @PostMapping("/repos/{owner}/{repo}/issues")
    fun createIssue(
        @PathVariable owner: String,
        @PathVariable repo: String,
        @RequestBody request: CreateIssueRequest
    ): GithubIssueDto
}

// DTOs
data class GithubUserDto(
    val login: String,
    val id: Long,
    val name: String?,
    val email: String?,
    val bio: String?
)

data class GithubRepoDto(
    val id: Long,
    val name: String,
    val full_name: String,
    val description: String?,
    val stargazers_count: Int
)

data class CreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = emptyList()
)

data class GithubIssueDto(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String
)
```

### Feign Configuration

```kotlin
// infrastructure/external/config/GithubClientConfig.kt
package xyz.robinjoon.growweek.integration.infrastructure.external.config

@Configuration
class GithubClientConfig {

    @Bean
    fun githubFeignLogger(): Logger.Level = Logger.Level.FULL

    @Bean
    fun requestInterceptor(
        @Value("\${github.api.token}") token: String
    ): RequestInterceptor {
        return RequestInterceptor { template ->
            template.header("Authorization", "Bearer $token")
            template.header("Accept", "application/vnd.github.v3+json")
        }
    }

    @Bean
    fun errorDecoder(): ErrorDecoder {
        return GithubErrorDecoder()
    }

    @Bean
    fun requestOptions(): Request.Options {
        return Request.Options(
            5000, TimeUnit.MILLISECONDS,  // connectTimeout
            10000, TimeUnit.MILLISECONDS  // readTimeout
        )
    }
}
```

### Error Decoder

```kotlin
// infrastructure/external/config/GithubErrorDecoder.kt
package xyz.robinjoon.growweek.integration.infrastructure.external.config

class GithubErrorDecoder : ErrorDecoder {

    private val objectMapper = ObjectMapper()

    override fun decode(methodKey: String, response: Response): Exception {
        val errorBody = response.body()?.asInputStream()?.use {
            objectMapper.readValue(it, GithubErrorResponse::class.java)
        }

        return when (response.status()) {
            401 -> UnauthorizedException("Invalid Github token")
            403 -> ForbiddenException("Github API rate limit exceeded")
            404 -> ResourceNotFoundException(
                errorBody?.message ?: "Resource not found"
            )
            422 -> ValidationException(
                errorBody?.message ?: "Validation failed"
            )
            else -> ExternalServiceException(
                "Github API error: ${response.status()}"
            )
        }
    }
}

data class GithubErrorResponse(
    val message: String,
    val documentation_url: String?
)
```

### 도메인 어댑터 패턴

```kotlin
// domain/service/GithubService.kt (인터페이스)
package xyz.robinjoon.growweek.integration.domain.service

interface GithubService {
    fun getUserInfo(username: String): UserInfo
    fun getRepositories(username: String): List<Repository>
}

// infrastructure/external/adapter/GithubServiceAdapter.kt
package xyz.robinjoon.growweek.integration.infrastructure.external.adapter

@Component
class GithubServiceAdapter(
    private val githubClient: GithubClient
) : GithubService {

    override fun getUserInfo(username: String): UserInfo {
        val dto = try {
            githubClient.getUser(username)
        } catch (e: FeignException) {
            throw ExternalServiceException(
                "Failed to fetch Github user: $username",
                e
            )
        }

        return UserInfo(
            username = dto.login,
            name = dto.name ?: dto.login,
            email = dto.email,
            bio = dto.bio
        )
    }

    override fun getRepositories(username: String): List<Repository> {
        val dtos = githubClient.getRepositories(username)

        return dtos.map { dto ->
            Repository(
                name = dto.name,
                fullName = dto.full_name,
                description = dto.description,
                stars = dto.stargazers_count
            )
        }
    }
}
```

### Fallback (Circuit Breaker)

```kotlin
// infrastructure/external/fallback/GithubClientFallback.kt
package xyz.robinjoon.growweek.integration.infrastructure.external.fallback

@Component
class GithubClientFallback : GithubClient {

    override fun getUser(username: String): GithubUserDto {
        // Fallback 응답
        return GithubUserDto(
            login = username,
            id = 0,
            name = "Unknown",
            email = null,
            bio = "Service temporarily unavailable"
        )
    }

    override fun getRepositories(
        username: String,
        page: Int,
        per_page: Int
    ): List<GithubRepoDto> {
        return emptyList()
    }

    override fun createIssue(
        owner: String,
        repo: String,
        request: CreateIssueRequest
    ): GithubIssueDto {
        throw ServiceUnavailableException("Github service is down")
    }
}

// Feign Client에 fallback 추가
@FeignClient(
    name = "github",
    url = "\${github.api.url}",
    configuration = [GithubClientConfig::class],
    fallback = GithubClientFallback::class
)
interface GithubClient {
    // ...
}
```

### application.yml 설정

```yaml
github:
  api:
    url: https://api.github.com
    token: ${GITHUB_TOKEN}

feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: FULL
      github:
        connectTimeout: 3000
        readTimeout: 5000
```
