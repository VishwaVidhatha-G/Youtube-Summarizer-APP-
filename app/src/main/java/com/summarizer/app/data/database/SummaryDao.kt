package com.summarizer.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the 'summaries' table.
 * Standardizes SQLite interactions using reactive flows.
 */
@Dao
interface SummaryDao {

    /**
     * Retrieves all cached summaries sorted by newest first.
     * Returns a reactive Flow that automatically emits updates whenever the database changes.
     */
    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    fun getAllSummariesFlow(): Flow<List<SummaryEntity>>

    /**
     * Checks if a video already has a cached summary.
     */
    @Query("SELECT * FROM summaries WHERE videoId = :videoId LIMIT 1")
    suspend fun getSummaryById(videoId: String): SummaryEntity?

    /**
     * Caches or updates a video summary.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)

    /**
     * Deletes a specific summary from the database.
     */
    @Query("DELETE FROM summaries WHERE videoId = :videoId")
    suspend fun deleteSummaryById(videoId: String)
}
