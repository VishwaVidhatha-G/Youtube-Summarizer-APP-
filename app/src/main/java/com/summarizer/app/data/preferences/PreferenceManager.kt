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

    /**
     * Retrieve the summarization template prompt. Fallback to [DEFAULT_PROMPT] if empty.
     */
    fun getCustomPrompt(): String {
        val stored = sharedPreferences.getString(KEY_CUSTOM_PROMPT, "")
        return if (stored.isNullOrBlank()) DEFAULT_PROMPT else stored
    }

    companion object {
        private const val KEY_API_KEY = "secure_gemini_api_key"
        private const val KEY_CUSTOM_PROMPT = "custom_summarizer_prompt"

        /**
         * The user's custom template prompt configured as the default.
         */
        val DEFAULT_PROMPT = """
            Provide a complete, comprehensive summary of this video transcript in simple, beginner-friendly English. 
            Categorize the information logically with bold headings (**Heading**).
            Under each heading, provide highly detailed bullet points covering EVERY update, feature, and detail mentioned in the video.
            Zero information loss and zero context loss is non-negotiable. 
            Format the output using standard markdown. Do not use hashes.
            
            Here is the transcript:
        """.trimIndent()
    }
}
