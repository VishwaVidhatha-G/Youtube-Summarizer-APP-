package com.summarizer.app.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages device configurations and security credentials securely.
 * Encrypts data at rest using AES-256 GCM backed by the Android Keystore system.
 */
class PreferenceManager(context: Context) {

    // Retrieve or generate a secure Master Key in Android Keystore
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Initialize the EncryptedSharedPreferences delegate
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Securely cache the Gemini API Key.
     */
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    /**
     * Fetch the API Key. Returns your personal API Key as default, or any custom key you set.
     */
    fun getApiKey(): String {
        val stored = sharedPreferences.getString(KEY_API_KEY, "")
        return if (stored.isNullOrBlank()) "" else stored
    }

    /**
     * Save the customized summarization template prompt.
     */
    fun saveCustomPrompt(prompt: String) {
        sharedPreferences.edit().putString(KEY_CUSTOM_PROMPT, prompt).apply()
    }

    fun getCustomPrompt(): String {
        val stored = sharedPreferences.getString(KEY_CUSTOM_PROMPT, "") ?: ""
        val oldKeywords = listOf(
            "Provide a complete, comprehensive summary",
            "Here is the transcript:",
            "Zero information loss and zero context loss is non-negotiable",
            "Summarise this video in simple, beginner friendly"
        )
        val needsMigration = stored.isBlank() || oldKeywords.any { stored.contains(it) }
        return if (needsMigration) DEFAULT_PROMPT else stored
    }

    companion object {
        private const val KEY_API_KEY = "secure_gemini_api_key"
        private const val KEY_CUSTOM_PROMPT = "custom_summarizer_prompt"

        /**
         * The user's custom template prompt configured as the default.
         */
        val DEFAULT_PROMPT = """
            You are an expert video summarizer. Your task is to provide a highly detailed, comprehensive summary of the video transcript.
            
            CRITICAL GUIDELINES:
            1. LANGUAGE: Use simple, plain, and easy-to-understand English. Avoid complex jargon, academic vocabulary, or convoluted sentence structures. Explain any technical terms simply so a beginner can understand them.
            2. DETAIL & CONTEXT: Zero information loss and zero context loss is non-negotiable. You must cover every key point, update, feature, reason, and detail mentioned in the transcript. Do not omit details for the sake of brevity.
            3. CHRONOLOGICAL ORDER: The summary must strictly follow the chronological flow of the video events. Summarize events and topics in the exact sequence they are introduced.
            4. STRUCTURE: Categorize the summary using logical sections with bold headings (**Heading**). Under each heading, use descriptive, highly detailed bullet points.
            5. FORMATTING: Use standard markdown. Do not use hashes (#) for headings.
        """.trimIndent()
    }
}
