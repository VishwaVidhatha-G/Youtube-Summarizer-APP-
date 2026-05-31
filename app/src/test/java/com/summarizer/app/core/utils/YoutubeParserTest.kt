package com.summarizer.app.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests to verify that the YouTube link and title extraction logic works
 * robustly across various sharing combinations (shorts, standard links, short links, etc.).
 * Demonstrates high quality testing coverage for the portfolio.
 */
class YoutubeParserTest {

    @Test
    fun extractVideoId_withStandardDesktopUrl_returnsId() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val videoId = YoutubeParser.extractVideoId(url)
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun extractVideoId_withStandardDesktopUrlAndParams_returnsId() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120s&list=WL"
        val videoId = YoutubeParser.extractVideoId(url)
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun extractVideoId_withShortLinkUrl_returnsId() {
        val url = "https://youtu.be/dQw4w9WgXcQ"
        val videoId = YoutubeParser.extractVideoId(url)
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun extractVideoId_withShortLinkUrlAndParams_returnsId() {
        val url = "https://youtu.be/dQw4w9WgXcQ?si=abcdefgh"
        val videoId = YoutubeParser.extractVideoId(url)
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun extractVideoId_withShortsUrl_returnsId() {
        val url = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        val videoId = YoutubeParser.extractVideoId(url)
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun extractVideoId_withShareSheetTextBlock_returnsId() {
        // Typical share caption from YouTube Android app
        val sharedText = "Check out this video: Rick Astley - Never Gonna Give You Up - https://youtu.be/dQw4w9WgXcQ"
        val videoId = YoutubeParser.extractVideoId(sharedText)
        assertEquals("dQw4w9WgXcQ", videoId)
    }

    @Test
    fun extractVideoId_withInvalidUrl_returnsNull() {
        val url = "https://google.com"
        val videoId = YoutubeParser.extractVideoId(url)
        assertNull(videoId)
    }

    @Test
    fun extractTitle_withQuotesInShareText_returnsTitle() {
        val sharedText = "Watch \"Why clean architecture matters\" on YouTube: https://youtu.be/dQw4w9WgXcQ"
        val title = YoutubeParser.extractTitle(sharedText)
        assertEquals("Why clean architecture matters", title)
    }

    @Test
    fun extractTitle_withoutQuotes_returnsDefault() {
        val sharedText = "Check out this video: https://youtu.be/dQw4w9WgXcQ"
        val title = YoutubeParser.extractTitle(sharedText)
        assertEquals("YouTube Video", title)
    }
}
