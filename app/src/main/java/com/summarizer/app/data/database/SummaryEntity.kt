package com.summarizer.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.summarizer.app.domain.model.SummaryItem

/**
 * Room Database entity representing a cached video summary.
 */
@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey 
    val videoId: String,
    val title: String,
    val summary: String,
    val timestamp: Long
) {
    /**
     * Map this Database Entity to the pure Domain Entity.
     */
    fun toDomain(): SummaryItem = SummaryItem(
        videoId = videoId,
        title = title,
        summary = summary,
        timestamp = timestamp
    )

    companion object {
        /**
         * Create a Database Entity from a pure Domain Entity.
         */
        fun fromDomain(item: SummaryItem): SummaryEntity = SummaryEntity(
            videoId = item.videoId,
            title = item.title,
            summary = item.summary,
            timestamp = item.timestamp
        )
    }
}
