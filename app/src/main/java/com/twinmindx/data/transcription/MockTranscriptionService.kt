package com.twinmindx.data.transcription

import kotlinx.coroutines.delay
import javax.inject.Inject

private val MOCK_SENTENCES = listOf(
    "Good morning everyone, let's get started with today's meeting.",
    "We have a few items on the agenda that I'd like to cover.",
    "First, let's review the progress from last week's sprint.",
    "The team has made significant progress on the feature development.",
    "We've resolved three critical bugs and shipped two new features.",
    "Customer satisfaction scores have improved by fifteen percent.",
    "Let's discuss the roadmap for the next quarter.",
    "We need to align on priorities before the end of this week.",
    "Engineering estimates suggest the project will take about six weeks.",
    "We should also plan for some buffer time given the complexity.",
    "The design team will have mockups ready by Thursday.",
    "Any questions or concerns before we wrap up?",
)

class MockTranscriptionService @Inject constructor() : TranscriptionService {

    override suspend fun transcribe(filePath: String, chunkIndex: Int): String {
        delay((100L..300L).random())

        val sentenceStart = (chunkIndex * 2) % MOCK_SENTENCES.size
        val line1 = MOCK_SENTENCES[sentenceStart % MOCK_SENTENCES.size]
        val line2 = MOCK_SENTENCES[(sentenceStart + 1) % MOCK_SENTENCES.size]

        return "$line1 $line2"
    }
}
