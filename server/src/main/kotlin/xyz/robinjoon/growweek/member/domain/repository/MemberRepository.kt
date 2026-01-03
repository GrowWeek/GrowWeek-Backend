package xyz.robinjoon.growweek.member.domain.repository

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.member.domain.model.Member
import xyz.robinjoon.growweek.member.domain.model.command.MemberCommand
import xyz.robinjoon.growweek.member.domain.model.query.MemberQuery

interface MemberRepository {
    fun saveAll(commands: List<MemberCommand>): List<Member>

    fun findAll(query: MemberQuery): Page<Member>
}
