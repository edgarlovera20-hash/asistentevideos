package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY dateTimestamp DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    fun getMeetingById(id: Long): Flow<MeetingEntity?>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingByIdSync(id: Long): MeetingEntity?

    @Transaction
    @Query("SELECT * FROM meetings WHERE id = :id")
    fun getMeetingWithDetails(id: Long): Flow<MeetingWithDetails?>

    @Transaction
    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingWithDetailsSync(id: Long): MeetingWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)

    @Query("DELETE FROM meeting_summaries WHERE meetingId = :meetingId")
    suspend fun deleteSummariesForMeeting(meetingId: Long)

    @Query("DELETE FROM agreements WHERE meetingId = :meetingId")
    suspend fun deleteAgreementsForMeeting(meetingId: Long)

    @Query("DELETE FROM chat_messages WHERE meetingId = :meetingId")
    suspend fun deleteChatMessagesForMeeting(meetingId: Long)

    @Query("DELETE FROM participants WHERE meetingId = :meetingId")
    suspend fun deleteParticipantsForMeeting(meetingId: Long)

    @Query("DELETE FROM visual_assets WHERE meetingId = :meetingId")
    suspend fun deleteVisualAssetsForMeeting(meetingId: Long)

    // ponytail: no FK/cascade on these tables (schema is still v1, unreleased) — explicit
    // per-table deletes avoid a migration. Add ON DELETE CASCADE once a real migration path exists.
    @Transaction
    suspend fun deleteMeetingCascade(meetingId: Long) {
        deleteParticipantsForMeeting(meetingId)
        deleteTranscriptsForMeeting(meetingId)
        deleteTasksForMeeting(meetingId)
        deleteAgreementsForMeeting(meetingId)
        deleteChatMessagesForMeeting(meetingId)
        deleteVisualAssetsForMeeting(meetingId)
        deleteSummariesForMeeting(meetingId)
        deleteMeetingById(meetingId)
    }

    // Participants
    @Query("SELECT * FROM participants WHERE meetingId = :meetingId")
    fun getParticipants(meetingId: Long): Flow<List<ParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    // Meeting Summaries
    @Query("SELECT * FROM meeting_summaries WHERE meetingId = :meetingId ORDER BY generatedAtTimestamp DESC LIMIT 1")
    fun getSummaryForMeeting(meetingId: Long): Flow<MeetingSummaryEntity?>

    @Query("SELECT * FROM meeting_summaries WHERE meetingId = :meetingId ORDER BY generatedAtTimestamp DESC LIMIT 1")
    suspend fun getSummaryForMeetingSync(meetingId: Long): MeetingSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: MeetingSummaryEntity): Long

    // Transcript Segments
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY timestampMs ASC")
    fun getTranscriptSegments(meetingId: Long): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY timestampMs ASC")
    suspend fun getTranscriptSegmentsSync(meetingId: Long): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptSegment(segment: TranscriptSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptSegments(segments: List<TranscriptSegmentEntity>)

    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteTranscriptsForMeeting(meetingId: Long)

    // Action Tasks
    @Query("SELECT * FROM action_tasks WHERE meetingId = :meetingId")
    fun getTasksForMeeting(meetingId: Long): Flow<List<ActionTaskEntity>>

    @Query("SELECT * FROM action_tasks ORDER BY isCompleted ASC, priority DESC")
    fun getAllTasks(): Flow<List<ActionTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ActionTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ActionTaskEntity>)

    @Update
    suspend fun updateTask(task: ActionTaskEntity)

    @Query("DELETE FROM action_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM action_tasks WHERE meetingId = :meetingId")
    suspend fun deleteTasksForMeeting(meetingId: Long)

    // Agreements
    @Query("SELECT * FROM agreements WHERE meetingId = :meetingId")
    fun getAgreementsForMeeting(meetingId: Long): Flow<List<AgreementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgreements(agreements: List<AgreementEntity>)

    // Chat
    @Query("SELECT * FROM chat_messages WHERE meetingId = :meetingId ORDER BY timestampMs ASC")
    fun getChatMessages(meetingId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    // Visual Assets & Intelligence Engine
    @Query("SELECT * FROM visual_assets WHERE meetingId = :meetingId ORDER BY timestampMs DESC")
    fun getVisualAssetsForMeeting(meetingId: Long): Flow<List<VisualAssetEntity>>

    @Query("SELECT * FROM visual_assets ORDER BY timestampMs DESC")
    fun getAllVisualAssets(): Flow<List<VisualAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisualAsset(asset: VisualAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisualAssets(assets: List<VisualAssetEntity>)

    @Query("DELETE FROM visual_assets WHERE id = :id")
    suspend fun deleteVisualAsset(id: Long)

    // Search Across Enterprise Memory
    @Query("""
        SELECT DISTINCT m.* FROM meetings m
        LEFT JOIN transcript_segments t ON m.id = t.meetingId
        LEFT JOIN participants p ON m.id = p.meetingId
        WHERE m.title LIKE '%' || :query || '%'
           OR m.executiveSummary LIKE '%' || :query || '%'
           OR m.category LIKE '%' || :query || '%'
           OR t.text LIKE '%' || :query || '%'
           OR p.name LIKE '%' || :query || '%'
        ORDER BY m.dateTimestamp DESC
    """)
    fun searchMeetings(query: String): Flow<List<MeetingEntity>>
}
