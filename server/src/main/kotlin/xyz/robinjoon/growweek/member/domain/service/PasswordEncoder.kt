package xyz.robinjoon.growweek.member.domain.service

import xyz.robinjoon.growweek.member.domain.model.EncodedPassword
import xyz.robinjoon.growweek.member.domain.model.RawPassword

interface PasswordEncoder {
    fun encode(rawPassword: RawPassword): EncodedPassword
    fun matches(rawPassword: RawPassword, encodedPassword: EncodedPassword): Boolean
}
