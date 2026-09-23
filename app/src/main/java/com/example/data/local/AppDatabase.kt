package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.GenerationTask
import com.example.data.model.ProjectType
import com.example.data.model.SceneClip
import com.example.data.model.TaskStage
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromProjectType(value: ProjectType): String = value.name

    @TypeConverter
    fun toProjectType(value: String): ProjectType = runCatching { ProjectType.valueOf(value) }.getOrDefault(ProjectType.IMAGE_TO_IMAGE)

    @TypeConverter
    fun fromGenerationStatus(value: GenerationStatus): String = value.name

    @TypeConverter
    fun toGenerationStatus(value: String): GenerationStatus = runCatching { GenerationStatus.valueOf(value) }.getOrDefault(GenerationStatus.IDLE)

    @TypeConverter
    fun fromTaskStage(value: TaskStage): String = value.name

    @TypeConverter
    fun toTaskStage(value: String): TaskStage = runCatching { TaskStage.valueOf(value) }.getOrDefault(TaskStage.PLANNING)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<GenerationProject>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: String): Flow<GenerationProject?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectDirect(id: String): GenerationProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: GenerationProject)

    @Update
    suspend fun updateProject(project: GenerationProject)

    @Delete
    suspend fun deleteProject(project: GenerationProject)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface SceneClipDao {
    @Query("SELECT * FROM scene_clips WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    fun getClipsForProject(projectId: String): Flow<List<SceneClip>>

    @Query("SELECT * FROM scene_clips WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    suspend fun getClipsForProjectDirect(projectId: String): List<SceneClip>

    @Query("SELECT * FROM scene_clips WHERE id = :id")
    suspend fun getClipByIdDirect(id: String): SceneClip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<SceneClip>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: SceneClip)

    @Update
    suspend fun updateClip(clip: SceneClip)

    /**
     * Update only the editable creative fields of a clip. Used when the user tweaks a
     * storyboard prompt / narration / camera move before (or after) rendering, so that a
     * concurrent generation pass writing status fields cannot be clobbered.
     */
    @Query("UPDATE scene_clips SET sceneTitle = :title, visualPrompt = :visualPrompt, cameraMovement = :cameraMovement, narration = :narration WHERE id = :clipId")
    suspend fun updateClipPrompt(
        clipId: String,
        title: String,
        visualPrompt: String,
        cameraMovement: String,
        narration: String
    )

    @Query("DELETE FROM scene_clips WHERE projectId = :projectId")
    suspend fun deleteClipsForProject(projectId: String)

    @Query("DELETE FROM scene_clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: String)
}

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): ChatSession?

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentSession(): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSessionTitle(id: String, title: String, updatedAt: Long)

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchSession(id: String, updatedAt: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionDirect(sessionId: String): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun countMessagesForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun countAllMessages(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET sessionId = :sessionId WHERE sessionId IS NULL")
    suspend fun assignOrphanMessages(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface GenerationTaskDao {
    @Query("SELECT * FROM generation_tasks WHERE id = :id")
    suspend fun getById(id: String): GenerationTask?

    @Query("SELECT * FROM generation_tasks WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForProject(projectId: String): GenerationTask?

    @Query("SELECT * FROM generation_tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<GenerationTask>>

    /** Non-terminal tasks only: these are the ones a launch-time resume scan must consider. */
    @Query("SELECT * FROM generation_tasks WHERE stage IN ('PLANNING', 'RENDERING', 'STITCHING') ORDER BY createdAt ASC")
    suspend fun getResumable(): List<GenerationTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: GenerationTask)

    @Query("UPDATE generation_tasks SET stage = :stage, remoteTaskId = :remoteTaskId, currentClipId = :currentClipId, currentSceneNumber = :currentSceneNumber, totalScenes = :totalScenes, pollCount = :pollCount, lastPolledAt = :lastPolledAt, lastMessage = :lastMessage, error = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(
        id: String,
        stage: TaskStage,
        remoteTaskId: String?,
        currentClipId: String?,
        currentSceneNumber: Int,
        totalScenes: Int,
        pollCount: Int,
        lastPolledAt: Long,
        lastMessage: String,
        error: String?,
        updatedAt: Long
    )

    @Query("DELETE FROM generation_tasks WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)
}

@Database(
    entities = [GenerationProject::class, SceneClip::class, ChatMessage::class, ChatSession::class, GenerationTask::class],
    version = 7, // v7: GenerationProject.styleReferenceImageUrl (per-film style anchor image)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sceneClipDao(): SceneClipDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun generationTaskDao(): GenerationTaskDao
}
