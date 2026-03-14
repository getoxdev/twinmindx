package com.twinmindx.data.api

import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject

class MockTranscriptionService @Inject constructor() : TranscriptionService {

    private val sampleSentences = listOf(
        "Welcome to today's meeting. We're here to discuss our quarterly performance.",
        "The team has made significant progress on the new feature rollout.",
        "Our user engagement metrics have increased by thirty percent this quarter.",
        "We need to address the infrastructure scaling concerns raised last week.",
        "The marketing campaign is showing promising results in the target demographic.",
        "Let's review the action items from the previous session before moving forward.",
        "Customer feedback has been overwhelmingly positive regarding the new interface.",
        "We're on track to meet our Q4 targets if we maintain the current pace.",
        "There are a few blockers we need to resolve in the coming sprint.",
        "The engineering team will present their technical roadmap in the next segment."
    )

    override suspend fun transcribe(audioFile: File, chunkIndex: Int): String {
        delay((1000L..2500L).random())
        val index = chunkIndex % sampleSentences.size
        val nextIndex = (chunkIndex + 1) % sampleSentences.size
        return "${sampleSentences[index]} ${sampleSentences[nextIndex]}"
    }
}
