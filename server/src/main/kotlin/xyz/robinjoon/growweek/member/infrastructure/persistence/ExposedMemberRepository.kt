package xyz.robinjoon.growweek.member.infrastructure.persistence

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.*
import xyz.robinjoon.growweek.member.domain.model.command.MemberCommand
import xyz.robinjoon.growweek.member.domain.model.query.MemberQuery
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository
import java.time.LocalDateTime

@Repository
class ExposedMemberRepository : MemberRepository {
    @Transactional
    override fun saveAll(commands: List<MemberCommand>): List<Member> {
        val savedMembers = mutableListOf<Member>()

        commands.forEach { command ->
            when (command) {
                is MemberCommand.CreateMember -> {
                    val now = LocalDateTime.now()
                    val insertedId =
                        MemberTable.insert {
                            it[email] = command.email.value
                            it[password] = command.password.value
                            it[nickname] = command.nickname.value
                            it[status] = MemberStatus.ACTIVE.name
                            it[createdAt] = now
                            it[updatedAt] = now
                        } get MemberTable.id

                    val createdMember =
                        MemberTable
                            .selectAll()
                            .where { MemberTable.id eq insertedId }
                            .map { it.toMember() }
                            .single()
                    savedMembers.add(createdMember)
                }

                is MemberCommand.UpdateMember -> {
                    val existingMember =
                        MemberTable
                            .selectAll()
                            .where { MemberTable.id eq command.memberId.value }
                            .map { it.toMember() }
                            .singleOrNull()
                            ?: throw IllegalArgumentException("Member not found: ${command.memberId.value}")

                    var updatedMember = existingMember
                    command.nickname?.let { updatedMember = updatedMember.updateNickname(it) }

                    MemberTable.update({ MemberTable.id eq command.memberId.value }) {
                        it[nickname] = updatedMember.nickname.value
                        it[updatedAt] = LocalDateTime.now()
                    }

                    val refreshedMember =
                        MemberTable
                            .selectAll()
                            .where { MemberTable.id eq command.memberId.value }
                            .map { it.toMember() }
                            .single()
                    savedMembers.add(refreshedMember)
                }

                is MemberCommand.DeactivateMember -> {
                    MemberTable.update({ MemberTable.id eq command.memberId.value }) {
                        it[status] = MemberStatus.INACTIVE.name
                        it[updatedAt] = LocalDateTime.now()
                    }

                    val deactivatedMember =
                        MemberTable
                            .selectAll()
                            .where { MemberTable.id eq command.memberId.value }
                            .map { it.toMember() }
                            .single()
                    savedMembers.add(deactivatedMember)
                }
            }
        }

        return savedMembers
    }

    @Transactional(readOnly = true)
    override fun findAll(query: MemberQuery): Page<Member> {
        val members =
            when (query) {
                is MemberQuery.ById -> {
                    MemberTable
                        .selectAll()
                        .where { MemberTable.id eq query.memberId.value }
                        .map { it.toMember() }
                }

                is MemberQuery.ByEmail -> {
                    MemberTable
                        .selectAll()
                        .where { MemberTable.email eq query.email.value }
                        .map { it.toMember() }
                }
            }

        return OffsetPage(
            items = members,
            page = 0,
            size = members.size.coerceAtLeast(1),
            totalPage = if (members.isEmpty()) 0 else 1,
        )
    }

    private fun ResultRow.toMember(): Member =
        Member.load(
            id = MemberId(this[MemberTable.id].value),
            email = Email(this[MemberTable.email]),
            password = Password(this[MemberTable.password]),
            nickname = Nickname(this[MemberTable.nickname]),
            status = MemberStatus.valueOf(this[MemberTable.status]),
            createdAt = this[MemberTable.createdAt],
            updatedAt = this[MemberTable.updatedAt],
        )
}
