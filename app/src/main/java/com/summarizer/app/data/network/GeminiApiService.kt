package com.summarizer.app.data.network

import com.summarizer.app.data.model.GeminiRequest
import com.summarizer.app.data.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit contract representing the Google Gemini API generative endpoint.
 */
interface GeminiApiService {

    /**
     * Sends a generation request to the Gemini 3.5 Flash model.
     */
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @Headers("Content-Type: application/json")
    @retrofit2.http.Streaming
    @POST("v1beta/models/gemini-3.5-flash:streamGenerateContent?alt=sse")
    suspend fun streamGenerateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): retrofit2.Response<okhttp3.ResponseBody>
}
