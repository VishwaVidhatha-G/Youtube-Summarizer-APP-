package com.summarizer.app.domain.model

/**
 * Domain model representing a message in the video Q&A chatbot conversation.
 */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false
)
