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
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.ProjectType
import com.example.data.model.SceneClip
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<SceneClip>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: SceneClip)

    @Update
    suspend fun updateClip(clip: SceneClip)

    @Query("DELETE FROM scene_clips WHERE projectId = :projectId")
    suspend fun deleteClipsForProject(projectId: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Database(
    entities = [GenerationProject::class, SceneClip::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sceneClipDao(): SceneClipDao
    abstract fun chatMessageDao(): ChatMessageDao
}
