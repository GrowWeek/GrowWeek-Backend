package xyz.robinjoon.growweek.retrospective.infrastructure.persistence

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.*
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.domain.model.*
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.model.query.RetrospectiveQuery
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import java.time.LocalDateTime
import java.time.YearMonth

@Repository
class ExposedRetrospectiveRepository : RetrospectiveRepository {

    @Transactional
    override fun saveAll(commands: List<RetrospectiveCommand>): List<Retrospective> {
        val savedRetrospectives = mutableListOf<Retrospective>()

        commands.forEach { command ->
            when (command) {
                is RetrospectiveCommand.CreateRetrospective -> {
                    val now = LocalDateTime.now()
                    val insertedId = RetrospectiveTable.insert {
                        it[userId] = command.memberId.value
                        it[startDate] = command.period.startDate
                        it[endDate] = command.period.endDate
                        it[status] = RetrospectiveStatus.TODO.name
                        it[questionCount] = command.questionCount.value
                        it[additionalNotes] = null
                        it[createdAt] = now
                        it[updatedAt] = now
                    } get RetrospectiveTable.id

                    val created = findRetrospectiveById(RetrospectiveId(insertedId.value))
                    savedRetrospectives.add(created)
                }

                is RetrospectiveCommand.GenerateQuestions -> {
                    RetrospectiveTable.update({ RetrospectiveTable.id eq command.retrospectiveId.value }) {
                        it[status] = RetrospectiveStatus.BEFORE_GENERATE_QUESTION.name
                        it[updatedAt] = LocalDateTime.now()
                    }
                    val updated = findRetrospectiveById(command.retrospectiveId)
                    savedRetrospectives.add(updated)
                }

                is RetrospectiveCommand.CompleteQuestionGeneration -> {
                    val now = LocalDateTime.now()
                    command.generatedQuestionContents.forEachIndexed { index, content ->
                        QuestionTable.insert {
                            it[retrospectiveId] = command.retrospectiveId.value
                            it[QuestionTable.content] = content
                            it[order] = index
                            it[createdAt] = now
                        }
                    }
                    RetrospectiveTable.update({ RetrospectiveTable.id eq command.retrospectiveId.value }) {
                        it[status] = RetrospectiveStatus.AFTER_GENERATE_QUESTION.name
                        it[updatedAt] = now
                    }
                    val updated = findRetrospectiveById(command.retrospectiveId)
                    savedRetrospectives.add(updated)
                }

                is RetrospectiveCommand.WriteAnswer -> {
                    val now = LocalDateTime.now()
                    val existingAnswer = AnswerTable.selectAll()
                        .where { AnswerTable.questionId eq command.questionId.value }
                        .singleOrNull()

                    if (existingAnswer != null) {
                        AnswerTable.update({ AnswerTable.questionId eq command.questionId.value }) {
                            it[content] = command.content
                            it[updatedAt] = now
                        }
                    } else {
                        AnswerTable.insert {
                            it[questionId] = command.questionId.value
                            it[content] = command.content
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                    }

                    val retrospective = findRetrospectiveById(command.retrospectiveId)
                    if (retrospective.status == RetrospectiveStatus.AFTER_GENERATE_QUESTION) {
                        RetrospectiveTable.update({ RetrospectiveTable.id eq command.retrospectiveId.value }) {
                            it[status] = RetrospectiveStatus.IN_PROGRESS.name
                            it[updatedAt] = now
                        }
                    }
                    val updated = findRetrospectiveById(command.retrospectiveId)
                    savedRetrospectives.add(updated)
                }

                is RetrospectiveCommand.WriteAdditionalNotes -> {
                    RetrospectiveTable.update({ RetrospectiveTable.id eq command.retrospectiveId.value }) {
                        it[additionalNotes] = command.notes.value
                        it[updatedAt] = LocalDateTime.now()
                    }
                    val updated = findRetrospectiveById(command.retrospectiveId)
                    savedRetrospectives.add(updated)
                }

                is RetrospectiveCommand.CompleteRetrospective -> {
                    RetrospectiveTable.update({ RetrospectiveTable.id eq command.retrospectiveId.value }) {
                        it[status] = RetrospectiveStatus.DONE.name
                        it[updatedAt] = LocalDateTime.now()
                    }
                    val updated = findRetrospectiveById(command.retrospectiveId)
                    savedRetrospectives.add(updated)
                }

                is RetrospectiveCommand.DeleteRetrospective -> {
                    val retrospectiveId = command.retrospectiveId.value
                    val questionIds = QuestionTable.selectAll()
                        .where { QuestionTable.retrospectiveId eq retrospectiveId }
                        .map { it[QuestionTable.id].value }

                    questionIds.forEach { qId ->
                        AnswerTable.deleteWhere { AnswerTable.questionId eq qId }
                    }
                    QuestionTable.deleteWhere { QuestionTable.retrospectiveId eq retrospectiveId }
                    RetrospectiveTable.deleteWhere { RetrospectiveTable.id eq retrospectiveId }
                }
            }
        }

        return savedRetrospectives
    }

    @Transactional(readOnly = true)
    override fun findAll(query: RetrospectiveQuery): Page<Retrospective> {
        return when (query.pageInfo) {
            is CursorPageInfo -> findWithCursor(query)
            is OffsetPageInfo -> findWithOffset(query)
        }
    }

    private fun findRetrospectiveById(id: RetrospectiveId): Retrospective {
        val row = RetrospectiveTable.selectAll()
            .where { RetrospectiveTable.id eq id.value }
            .single()
        return row.toRetrospective()
    }

    private fun findWithCursor(query: RetrospectiveQuery): CursorPage<Retrospective> {
        val pageInfo = query.pageInfo as CursorPageInfo
        var baseQuery = RetrospectiveTable.selectAll()

        when (query) {
            is RetrospectiveQuery.CursorByMemberId -> {
                baseQuery = baseQuery.andWhere { RetrospectiveTable.userId eq query.memberId.value }
            }

            is RetrospectiveQuery.CursorByMemberIdAndPeriod -> {
                baseQuery = baseQuery.andWhere {
                    (RetrospectiveTable.userId eq query.memberId.value) and
                            (RetrospectiveTable.startDate lessEq query.endDate) and
                            (RetrospectiveTable.endDate greaterEq query.startDate)
                }
            }

            is RetrospectiveQuery.CursorByRetrospectiveId -> {
                baseQuery = baseQuery.andWhere { RetrospectiveTable.id eq query.retrospectiveId.value }
            }

            is RetrospectiveQuery.CursorByMemberIdAndMonth -> {
                val yearMonth = YearMonth.of(query.year, query.month)
                val monthStart = yearMonth.atDay(1)
                val monthEnd = yearMonth.atEndOfMonth()
                baseQuery = baseQuery.andWhere {
                    (RetrospectiveTable.userId eq query.memberId.value) and
                            (RetrospectiveTable.startDate lessEq monthEnd) and
                            (RetrospectiveTable.endDate greaterEq monthStart)
                }
            }

            else -> {}
        }

        pageInfo.cursor?.let { cursor ->
            val cursorId = cursor.toLongOrNull()
            if (cursorId != null) {
                baseQuery = baseQuery.andWhere { RetrospectiveTable.id less cursorId }
            }
        }

        baseQuery = when (pageInfo.orderBy) {
            "createdAt" -> baseQuery.orderBy(RetrospectiveTable.createdAt to SortOrder.DESC)
            "startDate" -> baseQuery.orderBy(RetrospectiveTable.startDate to SortOrder.DESC)
            else -> baseQuery.orderBy(RetrospectiveTable.id to SortOrder.DESC)
        }

        val items = baseQuery
            .limit(pageInfo.size + 1)
            .map { it.toRetrospective() }

        val hasNext = items.size > pageInfo.size
        val resultItems = if (hasNext) items.dropLast(1) else items
        val nextCursor = if (hasNext) resultItems.lastOrNull()?.id?.value?.toString() else null

        return CursorPage(
            items = resultItems,
            cursor = pageInfo.cursor,
            size = pageInfo.size,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }

    private fun findWithOffset(query: RetrospectiveQuery): OffsetPage<Retrospective> {
        val pageInfo = query.pageInfo as OffsetPageInfo
        var baseQuery = RetrospectiveTable.selectAll()

        when (query) {
            is RetrospectiveQuery.OffsetByMemberId -> {
                baseQuery = baseQuery.andWhere { RetrospectiveTable.userId eq query.memberId.value }
            }

            is RetrospectiveQuery.OffsetByMemberIdAndPeriod -> {
                baseQuery = baseQuery.andWhere {
                    (RetrospectiveTable.userId eq query.memberId.value) and
                            (RetrospectiveTable.startDate lessEq query.endDate) and
                            (RetrospectiveTable.endDate greaterEq query.startDate)
                }
            }

            is RetrospectiveQuery.OffsetByRetrospectiveId -> {
                baseQuery = baseQuery.andWhere { RetrospectiveTable.id eq query.retrospectiveId.value }
            }

            is RetrospectiveQuery.OffsetByMemberIdAndMonth -> {
                val yearMonth = YearMonth.of(query.year, query.month)
                val monthStart = yearMonth.atDay(1)
                val monthEnd = yearMonth.atEndOfMonth()
                baseQuery = baseQuery.andWhere {
                    (RetrospectiveTable.userId eq query.memberId.value) and
                            (RetrospectiveTable.startDate lessEq monthEnd) and
                            (RetrospectiveTable.endDate greaterEq monthStart)
                }
            }

            else -> {}
        }

        baseQuery = when (pageInfo.orderBy) {
            "createdAt" -> baseQuery.orderBy(RetrospectiveTable.createdAt to SortOrder.DESC)
            "startDate" -> baseQuery.orderBy(RetrospectiveTable.startDate to SortOrder.DESC)
            else -> baseQuery.orderBy(RetrospectiveTable.id to SortOrder.DESC)
        }

        val totalCount = baseQuery.count().toInt()
        val totalPage = if (totalCount == 0) 0 else (totalCount - 1) / pageInfo.size + 1

        val items = baseQuery
            .limit(pageInfo.size)
            .offset((pageInfo.page * pageInfo.size).toLong())
            .map { it.toRetrospective() }

        return OffsetPage(
            items = items,
            page = pageInfo.page,
            size = pageInfo.size,
            totalPage = totalPage
        )
    }

    private fun ResultRow.toRetrospective(): Retrospective {
        val retrospectiveId = RetrospectiveId(this[RetrospectiveTable.id].value)

        val questions = QuestionTable.selectAll()
            .where { QuestionTable.retrospectiveId eq retrospectiveId.value }
            .orderBy(QuestionTable.order to SortOrder.ASC)
            .map { it.toQuestion(retrospectiveId) }

        val answers = questions.mapNotNull { question ->
            AnswerTable.selectAll()
                .where { AnswerTable.questionId eq question.id.value }
                .singleOrNull()
                ?.let { question.id to it.toAnswer(question.id) }
        }.toMap()

        return Retrospective(
            id = retrospectiveId,
            memberId = MemberId(this[RetrospectiveTable.userId]),
            period = RetrospectivePeriod(
                startDate = this[RetrospectiveTable.startDate],
                endDate = this[RetrospectiveTable.endDate]
            ),
            status = RetrospectiveStatus.valueOf(this[RetrospectiveTable.status]),
            questionCount = QuestionCount(this[RetrospectiveTable.questionCount]),
            questions = questions,
            answers = answers,
            additionalNotes = this[RetrospectiveTable.additionalNotes]?.let { AdditionalNotes(it) },
            createdAt = this[RetrospectiveTable.createdAt],
            updatedAt = this[RetrospectiveTable.updatedAt]
        )
    }

    private fun ResultRow.toQuestion(retrospectiveId: RetrospectiveId): Question {
        return Question(
            id = QuestionId(this[QuestionTable.id].value),
            retrospectiveId = retrospectiveId,
            content = this[QuestionTable.content],
            order = this[QuestionTable.order],
            createdAt = this[QuestionTable.createdAt]
        )
    }

    private fun ResultRow.toAnswer(questionId: QuestionId): Answer {
        return Answer(
            id = AnswerId(this[AnswerTable.id].value),
            questionId = questionId,
            content = this[AnswerTable.content],
            createdAt = this[AnswerTable.createdAt],
            updatedAt = this[AnswerTable.updatedAt]
        )
    }
}
