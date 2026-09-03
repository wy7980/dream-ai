package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.GenerationProject
import com.example.data.model.SceneClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaExportHelper {

    private const val TAG = "MediaExportHelper"

    /**
     * Save an image (local file, content URI, or remote HTTP URL) directly to the device's
     * system Photo Gallery (Pictures/AgnesAI).
     */
    suspend fun saveImageToGallery(
        context: Context,
        imageUriOrPath: String?,
        title: String? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (imageUriOrPath.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("图像路径为空，无法保存"))
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val displayName = "Agnes_AI_${timestamp}.jpg"

            // 1. Resolve bitmap or input stream
            val bitmap = loadBitmap(context, imageUriOrPath)
                ?: return@withContext Result.failure(IllegalStateException("无法解析或下载该图像数据"))

            // 2. Insert into MediaStore Images
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AgnesAI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                if (!title.isNullOrBlank()) {
                    put(MediaStore.Images.Media.DESCRIPTION, title)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("无法在系统相册中创建文件条目"))

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return@withContext Result.failure(IllegalStateException("写入相册失败"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            // Also trigger media scanner for full indexing
            val savedPath = getPathFromUri(context, uri)
            if (savedPath != null) {
                MediaScannerConnection.scanFile(context, arrayOf(savedPath), arrayOf("image/jpeg"), null)
            }

            Log.i(TAG, "Image successfully saved to MediaStore: $uri")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image to gallery", e)
            Result.failure(e)
        }
    }

    /**
     * Save video or stitched cinematic storyboard to the device system library
     * (Movies/AgnesAI or Pictures/AgnesAI).
     */
    suspend fun saveVideoProjectToGallery(
        context: Context,
        project: GenerationProject,
        clips: List<SceneClip>
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val videoPath = project.resultVideoUri
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // If a real video file exists on disk
            if (!videoPath.isNullOrBlank() && (videoPath.endsWith(".mp4") || File(videoPath).exists())) {
                val videoFile = File(videoPath)
                if (videoFile.exists() && videoFile.length() > 0) {
                    val displayName = "Agnes_Video_${timestamp}.mp4"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AgnesAI")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: return@withContext Result.failure(IllegalStateException("无法在系统视频库中创建文件"))

                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(videoFile).use { input ->
                            input.copyTo(out)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    return@withContext Result.success(uri)
                }
            }

            // If video is composed of multi-scene cinematic storyboard keyframes,
            // render a high-definition 4-Scene Cinematic Master Storyboard Montage Image!
            val storyboardBitmap = createCinematicMontageBitmap(context, project, clips)
            val displayName = "Agnes_Storyboard_${timestamp}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AgnesAI/Storyboards")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                put(MediaStore.Images.Media.DESCRIPTION, project.prompt)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("无法创建分镜画册条目"))

            resolver.openOutputStream(uri)?.use { out ->
                storyboardBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return@withContext Result.failure(IllegalStateException("写入分镜画册失败"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            // Also save each individual scene clip keyframe
            clips.forEach { clip ->
                val clipPath = clip.videoUrl ?: clip.previewThumbnailUrl
                if (!clipPath.isNullOrBlank()) {
                    saveImageToGallery(context, clipPath, "第 ${clip.sceneNumber} 幕: ${clip.sceneTitle}")
                }
            }

            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export video project", e)
            Result.failure(e)
        }
    }

    /**
     * Share image or video to external apps (WeChat, QQ, System Share Sheet, etc.)
     */
    fun shareMedia(
        context: Context,
        uriOrPath: String?,
        isVideo: Boolean = false,
        shareTitle: String = "分享 Agnes AI 创作"
    ) {
        if (uriOrPath.isNullOrBlank()) {
            Toast.makeText(context, "作品路径不存在，无法分享", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = if (uriOrPath.startsWith("content://")) {
                Uri.parse(uriOrPath)
            } else if (uriOrPath.startsWith("/")) {
                val file = File(uriOrPath)
                if (!file.exists()) {
                    Toast.makeText(context, "本地文件未找到", Toast.LENGTH_SHORT).show()
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.parse(uriOrPath)
            }

            val mimeType = if (isVideo) "video/*" else "image/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, "由 Agnes AI 全能创作智能体生成 ✨")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, shareTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing media", e)
            Toast.makeText(context, "调起分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open the saved media directly in the system gallery or video player.
     */
    fun openInSystemViewer(context: Context, uri: Uri, isVideo: Boolean = false) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (isVideo) "video/*" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open system viewer", e)
            Toast.makeText(context, "已成功保存！可在系统相册/文件管理器中查看", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadBitmap(context: Context, uriOrPath: String): Bitmap? {
        return try {
            if (uriOrPath.startsWith("/")) {
                BitmapFactory.decodeFile(uriOrPath)
            } else if (uriOrPath.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(uriOrPath))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else if (uriOrPath.startsWith("http://") || uriOrPath.startsWith("https://")) {
                val url = URL(uriOrPath)
                val conn = url.openConnection() as HttpURLConnection
                conn.doInput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.connect()
                conn.inputStream.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                BitmapFactory.decodeFile(uriOrPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from $uriOrPath", e)
            null
        }
    }

    private fun createCinematicMontageBitmap(
        context: Context,
        project: GenerationProject,
        clips: List<SceneClip>
    ): Bitmap {
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        canvas.drawColor(Color.parseColor("#080B12"))

        // Top Header
        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 34f
        paint.isFakeBoldText = true
        canvas.drawText("AGNES AI 🎬 电影分镜全景成片图谱", 40f, 60f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = false
        val promptDisplay = if (project.prompt.length > 60) project.prompt.take(60) + "..." else project.prompt
        canvas.drawText("构思主题: “$promptDisplay”", 40f, 100f, paint)

        val activeClips = if (clips.isNotEmpty()) clips else listOf()
        val count = activeClips.size.coerceAtMost(4)

        if (count > 0) {
            val gridCols = if (count <= 2) count else 2
            val gridRows = if (count <= 2) 1 else 2

            val cellWidth = (width - 80 - (gridCols - 1) * 20) / gridCols
            val cellHeight = (height - 140 - (gridRows - 1) * 20) / gridRows

            for (i in 0 until count) {
                val clip = activeClips[i]
                val row = i / gridCols
                val col = i % gridCols

                val left = 40 + col * (cellWidth + 20)
                val top = 130 + row * (cellHeight + 20)
                val right = left + cellWidth
                val bottom = top + cellHeight

                // Draw Scene Frame
                val clipUri = clip.videoUrl ?: clip.previewThumbnailUrl
                val sceneBmp = clipUri?.let { loadBitmap(context, it) }

                if (sceneBmp != null) {
                    canvas.drawBitmap(sceneBmp, Rect(0, 0, sceneBmp.width, sceneBmp.height), Rect(left, top, right, bottom), paint)
                } else {
                    paint.color = Color.parseColor("#161E31")
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                }

                // Scene Border
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.parseColor("#38BDF8")
                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)

                // Scene Overlay Tag
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(190, 0, 0, 0)
                canvas.drawRect(left.toFloat(), top.toFloat(), left + 260f, top + 42f, paint)

                paint.color = Color.WHITE
                paint.textSize = 20f
                paint.isFakeBoldText = true
                canvas.drawText("第 0${clip.sceneNumber} 幕 • ${clip.sceneTitle}", left + 12f, top + 28f, paint)

                // Subtitle
                val narration = clip.narration.ifBlank { clip.visualPrompt.take(30) }
                paint.color = Color.argb(210, 0, 0, 0)
                canvas.drawRect(left.toFloat(), bottom - 38f, right.toFloat(), bottom.toFloat(), paint)

                paint.color = Color.parseColor("#FDE047")
                paint.textSize = 18f
                val subText = if (narration.length > 32) narration.take(32) + "..." else narration
                canvas.drawText(subText, left + 14f, bottom - 12f, paint)
            }
        }

        return bitmap
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
