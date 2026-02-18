package xyz.robinjoon.growweek.common.infrastructure.security

import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import xyz.robinjoon.growweek.common.presentation.security.CurrentMemberId

@Component
class CurrentMemberIdArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentMemberId::class.java) &&
            parameter.parameterType == Long::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val authentication =
            SecurityContextHolder.getContext().authentication
                as? JwtAuthenticationToken
                ?: throw IllegalStateException("인증 정보가 존재하지 않습니다.")
        return authentication.memberId.value
    }
}
