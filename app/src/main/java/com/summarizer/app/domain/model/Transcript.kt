package com.summarizer.app.domain.model

/**
 * Domain entity representing the extracted subtitle/transcript tracks of a YouTube video.
 */
data class TranscriptSegment(
    val text: String,
    val startMs: Long,
    val durationMs: Long
)

data class Transcript(
    val videoId: String,
    val title: String,
    val segments: List<TranscriptSegment>
) {
    /**
     * Concatenates all transcript lines into a single readable block of text for the LLM.
     */
    fun getFullText(): String = segments.joinToString(" ") { it.text.trim() }
}
