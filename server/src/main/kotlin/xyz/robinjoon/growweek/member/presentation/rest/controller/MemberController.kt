package xyz.robinjoon.growweek.member.presentation.rest.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import xyz.robinjoon.growweek.member.application.command.MemberApplicationCommand
import xyz.robinjoon.growweek.member.application.usecase.GetMemberUseCase
import xyz.robinjoon.growweek.member.application.usecase.LoginUseCase
import xyz.robinjoon.growweek.member.application.usecase.SignUpUseCase
import xyz.robinjoon.growweek.member.presentation.rest.request.LoginRequest
import xyz.robinjoon.growweek.member.presentation.rest.request.SignUpRequest
import xyz.robinjoon.growweek.member.presentation.rest.response.MemberResponse
import xyz.robinjoon.growweek.member.presentation.rest.response.TokenResponse

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "회원 API")
class MemberController(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val getMemberUseCase: GetMemberUseCase,
) {
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다")
    fun signUp(
        @RequestBody request: SignUpRequest,
    ): ResponseEntity<MemberResponse> {
        val command =
            MemberApplicationCommand.SignUp(
                email = request.email,
                password = request.password,
                nickname = request.nickname,
            )
        val member = signUpUseCase.signUp(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member))
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다")
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<TokenResponse> {
        val command =
            MemberApplicationCommand.Login(
                email = request.email,
                password = request.password,
            )
        val token = loginUseCase.login(command)
        return ResponseEntity.ok(TokenResponse.from(token))
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 조회", description = "로그인된 사용자의 정보를 조회합니다")
    fun getCurrentMember(authentication: Authentication): ResponseEntity<MemberResponse> {
        val memberId =
            authentication.name.toLongOrNull()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val member =
            getMemberUseCase.getMember(memberId)
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(MemberResponse.from(member))
    }
}
