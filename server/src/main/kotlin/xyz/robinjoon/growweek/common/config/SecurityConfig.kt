package xyz.robinjoon.growweek.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import xyz.robinjoon.growweek.common.infrastructure.security.JwtAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
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
                        "/webjars/**",
                        "/actuator/**",
                    ).permitAll()
                    // H2 Console 허용
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    // 인증 없이 접근 가능한 API
                    .requestMatchers(
                        "/api/v1/members/signup",
                        "/api/v1/members/login",
                    ).permitAll()
                    // 나머지 API는 인증 필요
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { it.frameOptions { frame -> frame.sameOrigin() } }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000", "https://dev.growweek.robinjoon.xyz")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
