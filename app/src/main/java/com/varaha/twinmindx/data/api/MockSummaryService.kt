package com.varaha.twinmindx.data.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MockSummaryService @Inject constructor() : SummaryService {

    override fun summarize(transcript: String): Flow<SummaryChunk> = flow {
        delay(300)
        emit(SummaryChunk(title = "Q4 Strategy & Performance Review"))
        delay(400)

        val summaryParts = listOf(
            "The team convened to review quarterly performance metrics and strategic initiatives. ",
            "User engagement has grown 30% this quarter, driven by the new interface rollout. ",
            "Infrastructure scaling remains a key concern requiring immediate attention. ",
            "Marketing campaigns are performing above expectations in target demographics."
        )
        var accumulated = ""
        for (part in summaryParts) {
            accumulated += part
            emit(SummaryChunk(summary = accumulated))
            delay(200)
        }

        delay(300)
        val actionItemParts = listOf(
            "• Resolve infrastructure scaling blockers before the next sprint\n",
            "• Present technical roadmap to stakeholders by end of week\n",
            "• Follow up on customer feedback items logged in the support queue\n",
            "• Schedule Q1 planning session with all team leads"
        )
        var accumulatedActions = ""
        for (part in actionItemParts) {
            accumulatedActions += part
            emit(SummaryChunk(actionItems = accumulatedActions))
            delay(150)
        }

        delay(300)
        val keyPointParts = listOf(
            "• 30% increase in user engagement metrics this quarter\n",
            "• New feature rollout completed ahead of schedule\n",
            "• Q4 targets remain achievable at current pace\n",
            "• Customer satisfaction scores at an all-time high"
        )
        var accumulatedPoints = ""
        for (part in keyPointParts) {
            accumulatedPoints += part
            emit(SummaryChunk(keyPoints = accumulatedPoints))
            delay(150)
        }
    }
}
