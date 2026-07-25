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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)

    @Query("DELETE FROM participants WHERE meetingId = :meetingId")
    suspend fun deleteParticipantsForMeeting(meetingId: Long)

    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteTranscriptSegmentsForMeeting(meetingId: Long)

    @Query("DELETE FROM action_tasks WHERE meetingId = :meetingId")
    suspend fun deleteTasksForMeeting(meetingId: Long)

    @Query("DELETE FROM agreements WHERE meetingId = :meetingId")
    suspend fun deleteAgreementsForMeeting(meetingId: Long)

    @Query("DELETE FROM chat_messages WHERE meetingId = :meetingId")
    suspend fun deleteChatMessagesForMeeting(meetingId: Long)

    // ponytail: no FK/cascade on these tables (schema is still v1, unreleased) — explicit
    // per-table deletes avoid a migration. Add ON DELETE CASCADE once a real migration path exists.
    @Transaction
    suspend fun deleteMeetingCascade(meetingId: Long) {
        deleteParticipantsForMeeting(meetingId)
        deleteTranscriptSegmentsForMeeting(meetingId)
        deleteTasksForMeeting(meetingId)
        deleteAgreementsForMeeting(meetingId)
        deleteChatMessagesForMeeting(meetingId)
        deleteMeetingById(meetingId)
    }

    // Participants
    @Query("SELECT * FROM participants WHERE meetingId = :meetingId")
    fun getParticipants(meetingId: Long): Flow<List<ParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    // Transcript Segments
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY timestampMs ASC")
    fun getTranscriptSegments(meetingId: Long): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY timestampMs ASC")
    suspend fun getTranscriptSegmentsSync(meetingId: Long): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptSegment(segment: TranscriptSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptSegments(segments: List<TranscriptSegmentEntity>)

    // Action Tasks
    @Query("SELECT * FROM action_tasks WHERE meetingId = :meetingId")
    fun getTasksForMeeting(meetingId: Long): Flow<List<ActionTaskEntity>>

    @Query("SELECT * FROM action_tasks ORDER BY isCompleted ASC, priority DESC")
    fun getAllTasks(): Flow<List<ActionTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ActionTaskEntity>)

    @Update
    suspend fun updateTask(task: ActionTaskEntity)

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
