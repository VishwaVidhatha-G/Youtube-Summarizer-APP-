package com.summarizer.app.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.summarizer.app.domain.model.Transcript
import com.summarizer.app.domain.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * High-performance, client-side scraper to extract subtitles/transcripts from YouTube.
 * Runs completely on-device, mimicking browser behavior to ensure reliable extraction
 * without API keys or proxy servers.
 */
class YoutubeTranscriptScraper(private val okHttpClient: OkHttpClient) {
    private val gson = Gson()
    private val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Scrapes and parses the transcript of a given YouTube Video ID.
     * Operates on [Dispatchers.IO] to keep network tasks completely isolated from the UI thread.
     */
    suspend fun fetchTranscript(videoId: String): Result<Transcript> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            
            // Step 1: Fetch HTML to extract INNERTUBE_API_KEY
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch YouTube page. HTTP Code: ${response.code}"))
            }

            val html = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response body received from YouTube"))

            // Extract API Key
            val apiKeyRegex = "\"INNERTUBE_API_KEY\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"".toRegex()
            val apiMatch = apiKeyRegex.find(html)
                ?: return@withContext Result.failure(Exception("Could not extract INNERTUBE_API_KEY from YouTube HTML."))
            val apiKey = apiMatch.groupValues[1]

            // Scrape actual video title from HTML <title> tag
            val titleRegex = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
            val titleMatch = titleRegex.find(html)
            var scrapedTitle = titleMatch?.groupValues?.get(1)?.replace(" - YouTube", "")?.trim() ?: "YouTube Video"
            scrapedTitle = scrapedTitle
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")

            // Step 2: Use Android Innertube Client to bypass PoToken (&exp=xpe) requirements
            val innertubeUrl = "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
            val jsonPayload = """
                {
                  "context": {
                    "client": {
                      "clientName": "ANDROID",
                      "clientVersion": "20.10.38"
                    }
                  },
                  "videoId": "$videoId"
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)
            val innertubeRequest = Request.Builder()
                .url(innertubeUrl)
                .post(body)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .build()

            val innertubeResponse = okHttpClient.newCall(innertubeRequest).execute()
            val innertubeJson = innertubeResponse.body?.string() ?: return@withContext Result.failure(IOException("Empty response from Innertube API"))
            
            val jsonObject = org.json.JSONObject(innertubeJson)
            val captionTracksArray = jsonObject
                .optJSONObject("captions")
                ?.optJSONObject("playerCaptionsTracklistRenderer")
                ?.optJSONArray("captionTracks")

            if (captionTracksArray == null || captionTracksArray.length() == 0) {
                return@withContext Result.failure(
                    Exception("No subtitle tracks found for this video via Innertube API.")
                )
            }

            // Priority:
            // 1. Standard English (manual) ("en", but not starting with "a.")
            // 2. Auto-generated English ("en" with a.en vssId)
            // 3. Any English-family subtitle ("en-US", etc.)
            // 4. Fallback to the first available track
            var selectedBaseUrl: String? = null
            for (i in 0 until captionTracksArray.length()) {
                val track = captionTracksArray.getJSONObject(i)
                val langCode = track.optString("languageCode", "")
                val vssId = track.optString("vssId", "")
                if (langCode == "en" && !vssId.startsWith("a.")) {
                    selectedBaseUrl = track.optString("baseUrl")
                    break
                }
            }
            if (selectedBaseUrl == null) {
                for (i in 0 until captionTracksArray.length()) {
                    val track = captionTracksArray.getJSONObject(i)
                    val langCode = track.optString("languageCode", "")
                    if (langCode == "en" || langCode.startsWith("en")) {
                        selectedBaseUrl = track.optString("baseUrl")
                        break
                    }
                }
            }
            if (selectedBaseUrl == null) {
                selectedBaseUrl = captionTracksArray.getJSONObject(0).optString("baseUrl")
            }

            if (selectedBaseUrl.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Could not extract a valid subtitle URL from Innertube API."))
            }

            // Android API returns fmt=srv3 by default. Replace it with fmt=json3 for easy Gson parsing.
            var timedtextUrl = selectedBaseUrl
            if (timedtextUrl.contains("fmt=srv3")) {
                timedtextUrl = timedtextUrl.replace("fmt=srv3", "fmt=json3")
            } else if (!timedtextUrl.contains("fmt=json3")) {
                timedtextUrl += "&fmt=json3"
            }

            // Step 3: Fetch the actual JSON3 timedtext
            val timedtextRequest = Request.Builder()
                .url(timedtextUrl)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .build()

            val timedtextResponse = okHttpClient.newCall(timedtextRequest).execute()
            if (!timedtextResponse.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to retrieve timed subtitles. HTTP Code: ${timedtextResponse.code}"))
            }

            val json3String = timedtextResponse.body?.string() ?: return@withContext Result.failure(IOException("Subtitles response body was null"))
            
            if (json3String.isBlank()) {
                return@withContext Result.failure(Exception("YouTube subtitles returned an empty response. URL: $timedtextUrl"))
            }
            
            val timedtextDto = gson.fromJson(json3String, TimedTextDto::class.java)
                ?: return@withContext Result.failure(Exception("YouTube subtitles returned an invalid JSON payload: ${json3String.take(100)}..."))

            val segments = mutableListOf<TranscriptSegment>()
            timedtextDto.events?.forEach { event ->
                // Combine word/segment structures into standard subtitle lines
                val text = event.segs?.joinToString("") { it.utf8 ?: "" }
                if (!text.isNullOrBlank()) {
                    segments.add(
                        TranscriptSegment(
                            text = text.replace("\n", " ").trim(),
                            startMs = event.tStartMs ?: 0L,
                            durationMs = event.dDurationMs ?: 0L
                        )
                    )
                }
            }

            Result.success(Transcript(videoId, scrapedTitle, segments))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gson mapping models for metadata
    private data class CaptionTrackDto(
        val baseUrl: String,
        val languageCode: String,
        val vssId: String
    )

    // Gson mapping models for JSON3 subtitle tracks
    private data class TimedTextDto(
        val events: List<EventDto>?
    )

    private data class EventDto(
        val tStartMs: Long?,
        val dDurationMs: Long?,
        val segs: List<SegmentDto>?
    )

    private data class SegmentDto(
        val utf8: String?
    )
}
