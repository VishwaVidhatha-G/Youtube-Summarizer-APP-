package com.summarizer.app.data.model

/**
 * Data structures to map requests and responses for the Google Gemini API.
 */

data class GeminiRequest(
    val contents: List<ContentDto>
) {
    companion object {
        /**
         * Convenience builder to create a single-turn text generation request.
         */
        fun createSimpleRequest(prompt: String): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    ContentDto(
                        parts = listOf(
                            PartDto(text = prompt)
                        )
                    )
                )
            )
        }
    }
}

data class ContentDto(
    val parts: List<PartDto>
)

data class PartDto(
    val text: String
)

data class GeminiResponse(
    val candidates: List<CandidateDto>?
) {
    /**
     * Extracts the generated completion text from the API response safely.
     */
    fun getGeneratedText(): String? {
        return candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    }
}

data class CandidateDto(
    val content: ContentDto?,
    val finishReason: String?
)
