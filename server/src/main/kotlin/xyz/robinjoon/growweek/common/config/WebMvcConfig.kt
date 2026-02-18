package xyz.robinjoon.growweek.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import xyz.robinjoon.growweek.common.infrastructure.security.CurrentMemberIdArgumentResolver

@Configuration
class WebMvcConfig(
    private val currentMemberIdArgumentResolver: CurrentMemberIdArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentMemberIdArgumentResolver)
    }
}
