package com.summarizer.app.core.utils

/**
 * Utility to extract Video IDs and Titles from raw shared text blocks.
 */
object YoutubeParser {
    
    /**
     * Extracts the 11-character YouTube Video ID from standard, short, mobile, or shorts links.
     */
    fun extractVideoId(text: String?): String? {
        if (text.isNullOrBlank()) return null
        
        // Isolate URL from the shared text block using simple white-space boundary search
        val urlRegex = "(https?://[^\\s]+)".toRegex()
        val urlMatch = urlRegex.find(text)?.value ?: text
        
        // Capture 11-character alphanumeric block following standard YouTube URL routes
        val patterns = listOf(
            "youtube\\.com/watch\\?v=([^#&?\\s]{11})".toRegex(RegexOption.IGNORE_CASE),
            "youtu\\.be/([^#&?\\s]{11})".toRegex(RegexOption.IGNORE_CASE),
            "youtube\\.com/shorts/([^#&?\\s]{11})".toRegex(RegexOption.IGNORE_CASE),
            "youtube\\.com/embed/([^#&?\\s]{11})".toRegex(RegexOption.IGNORE_CASE),
            "youtube\\.com/v/([^#&?\\s]{11})".toRegex(RegexOption.IGNORE_CASE),
            "(?:v=)?([^#&?\\s]{11})".toRegex(RegexOption.IGNORE_CASE) // Fallback raw matcher
        )
        
        for (pattern in patterns) {
            val match = pattern.find(urlMatch)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Guesses the video title from sharing headers.
     * Often YouTube shares text like: Watch "Title of Video" on YouTube...
     */
    fun extractTitle(text: String?): String {
        if (text.isNullOrBlank()) return "YouTube Video"
        
        // Find text enclosed in double quotes
        val quoteRegex = "\"([^\"]+)\"".toRegex()
        val match = quoteRegex.find(text)
        if (match != null && match.groupValues.size > 1) {
            val parsedTitle = match.groupValues[1].trim()
            if (parsedTitle.isNotBlank()) {
                return parsedTitle
            }
        }
        
        return "YouTube Video"
    }
}
