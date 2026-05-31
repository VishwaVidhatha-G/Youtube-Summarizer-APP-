package com.summarizer.app.domain.model

/**
 * Domain entity representing a completed YouTube video summary.
 * Crucial for decoupling the presentation layer from the database and networking structures.
 */
data class SummaryItem(
    val videoId: String,
    val title: String,
    val summary: String,
    val timestamp: Long
)
