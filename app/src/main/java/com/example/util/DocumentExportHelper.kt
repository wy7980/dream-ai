package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result of a generated document file.
 */
data class GeneratedDocument(
    val file: File,
    val uri: Uri,
    val title: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val mimeType: String,
    val docType: DocumentType,
    val pageOrRowCount: Int = 1
)

enum class DocumentType {
    WORD,
    PDF,
    EXCEL
}

object DocumentExportHelper {

    private const val TAG = "DocumentExportHelper"

    /**
     * Format file size to human readable string (KB, MB).
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
            else -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f))
        }
    }

    private fun getDocumentsDir(context: Context): File {
        val dir = File(context.filesDir, "generated_docs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 1. Create a true vector PDF Document using Android's native PdfDocument.
     * Standard A4 page size: 595 x 842 pt.
     */
    suspend fun createPdfDocument(
        context: Context,
        title: String,
        content: String,
        author: String = "Dream AI Agent"
    ): Result<GeneratedDocument> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(30)
            val fileName = "${cleanTitle}_$timestamp.pdf"
            val file = File(getDocumentsDir(context), fileName)

            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50f
            val printableWidth = (pageWidth - margin * 2).toInt()

            // Paint styles
            val titlePaint = TextPaint().apply {
                color = Color.rgb(20, 30, 55)
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val metaPaint = TextPaint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 10f
                isAntiAlias = true
            }

            val headingPaint = TextPaint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 11f
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
                isAntiAlias = true
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = margin

            // Draw Document Header on first page
            canvas.drawText(title, margin, currentY + 16f, titlePaint)
            currentY += 28f

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("作者 / 生成者: $author   |   生成时间: $dateStr", margin, currentY + 8f, metaPaint)
            currentY += 18f

            // Divider line
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 16f

            // Parse content line by line
            val lines = content.split("\n")
            for (rawLine in lines) {
                val line = rawLine.trimEnd()
                if (line.isBlank()) {
                    currentY += 10f
                    continue
                }

                val isHeading = line.startsWith("#") || line.startsWith("【") || (line.length < 25 && line.endsWith("："))
                val paintToUse = if (isHeading) headingPaint else bodyPaint
                val cleanLine = line.removePrefix("#").removePrefix("#").removePrefix("#").trim()

                // StaticLayout for multi-line text wrapping
                val layout = StaticLayout.Builder.obtain(cleanLine, 0, cleanLine.length, paintToUse, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(3f, 1f)
                    .build()

                // Check if page overflow
                if (currentY + layout.height + margin > pageHeight) {
                    // Draw Footer on current page
                    val footerText = "第 $currentPageNumber 页   |   Dream AI 智能文档中心"
                    canvas.drawText(footerText, margin, pageHeight - 30f, metaPaint)

                    pdfDocument.finishPage(page)
                    currentPageNumber++

                    // Start new page
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin

                    // Top header line on subsequent pages
                    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                    currentY += 16f
                }

                canvas.save()
                canvas.translate(margin, currentY)
                layout.draw(canvas)
                canvas.restore()

                currentY += layout.height + (if (isHeading) 8f else 4f)
            }

            // Draw Footer on last page
            val footerText = "第 $currentPageNumber 页   |   Dream AI 智能文档中心"
            canvas.drawText(footerText, margin, pageHeight - 30f, metaPaint)
            pdfDocument.finishPage(page)

            // Save to file
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            Result.success(
                GeneratedDocument(
                    file = file,
                    uri = uri,
                    title = title,
                    fileName = fileName,
                    fileSizeBytes = file.length(),
                    formattedSize = formatFileSize(file.length()),
                    mimeType = "application/pdf",
                    docType = DocumentType.PDF,
                    pageOrRowCount = currentPageNumber
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PDF", e)
            Result.failure(e)
        }
    }

    /**
     * 2. Create a professional Word Document (.doc / .docx compatible).
     * Emits standard Microsoft Office HTML-Word MHTML structure that opens seamlessly
     * in Microsoft Word, WPS Office, and Google Docs with full typography and styling.
     */
    suspend fun createWordDocument(
        context: Context,
        title: String,
        content: String,
        author: String = "Dream AI Agent"
    ): Result<GeneratedDocument> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(30)
            val fileName = "${cleanTitle}_$timestamp.doc"
            val file = File(getDocumentsDir(context), fileName)

            val dateStr = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date())

            // Convert Markdown content into styled HTML paragraphs
            val htmlContentBuilder = StringBuilder()
            val lines = content.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("### ") -> {
                        htmlContentBuilder.append("<h3>${trimmed.removePrefix("### ")}</h3>\n")
                    }
                    trimmed.startsWith("## ") -> {
                        htmlContentBuilder.append("<h2>${trimmed.removePrefix("## ")}</h2>\n")
                    }
                    trimmed.startsWith("# ") -> {
                        htmlContentBuilder.append("<h1>${trimmed.removePrefix("# ")}</h1>\n")
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        htmlContentBuilder.append("<p style='margin-left: 20pt; text-indent: -10pt;'>• ${trimmed.substring(2)}</p>\n")
                    }
                    trimmed.isBlank() -> {
                        htmlContentBuilder.append("<p style='margin: 6pt 0;'>&nbsp;</p>\n")
                    }
                    else -> {
                        // Bold tags conversion
                        val styledLine = trimmed.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
                        htmlContentBuilder.append("<p style='line-height: 1.6; margin: 6pt 0; text-indent: 2em;'>$styledLine</p>\n")
                    }
                }
            }

            val docContent = """
                <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <!--[if gte mso 9]>
                <xml>
                <w:WordDocument>
                <w:View>Print</w:View>
                <w:Zoom>100</w:Zoom>
                <w:DoNotOptimizeForBrowser/>
                </w:WordDocument>
                </xml>
                <![endif]-->
                <style>
                @page {
                    size: A4;
                    margin: 2.54cm 3.17cm 2.54cm 3.17cm;
                    mso-header-margin: 1.5cm;
                    mso-footer-margin: 1.75cm;
                }
                body {
                    font-family: 'PingFang SC', 'Microsoft YaHei', 'SimSun', sans-serif;
                    font-size: 12pt;
                    color: #1e293b;
                }
                .doc-title {
                    font-size: 22pt;
                    font-weight: bold;
                    color: #0f172a;
                    text-align: center;
                    margin-top: 18pt;
                    margin-bottom: 12pt;
                }
                .doc-meta {
                    font-size: 10pt;
                    color: #64748b;
                    text-align: center;
                    margin-bottom: 24pt;
                    border-bottom: 1.5pt solid #cbd5e1;
                    padding-bottom: 10pt;
                }
                h1 {
                    font-size: 16pt;
                    color: #1e3a8a;
                    margin-top: 18pt;
                    margin-bottom: 8pt;
                    border-left: 4pt solid #3b82f6;
                    padding-left: 8pt;
                }
                h2 {
                    font-size: 14pt;
                    color: #1e40af;
                    margin-top: 14pt;
                    margin-bottom: 6pt;
                }
                h3 {
                    font-size: 12pt;
                    font-weight: bold;
                    color: #334155;
                    margin-top: 10pt;
                    margin-bottom: 4pt;
                }
                .footer {
                    margin-top: 30pt;
                    border-top: 1pt dashed #cbd5e1;
                    padding-top: 8pt;
                    font-size: 9pt;
                    color: #94a3b8;
                    text-align: right;
                }
                </style>
                </head>
                <body>
                <div class="doc-title">$title</div>
                <div class="doc-meta">作者 / 规划者: $author &nbsp;&nbsp;|&nbsp;&nbsp; 日期: $dateStr &nbsp;&nbsp;|&nbsp;&nbsp; Dream AI 生成</div>
                $htmlContentBuilder
                <div class="footer">本规范公文由 Dream AI 智能体自主排版生成</div>
                </body>
                </html>
            """.trimIndent()

            FileOutputStream(file).use { out ->
                out.write(docContent.toByteArray(StandardCharsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            Result.success(
                GeneratedDocument(
                    file = file,
                    uri = uri,
                    title = title,
                    fileName = fileName,
                    fileSizeBytes = file.length(),
                    formattedSize = formatFileSize(file.length()),
                    mimeType = "application/msword",
                    docType = DocumentType.WORD,
                    pageOrRowCount = lines.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Word document", e)
            Result.failure(e)
        }
    }

    /**
     * 3. Create an Excel Spreadsheet (.csv / .xls compatible with UTF-8 BOM).
     * Includes UTF-8 BOM so Excel opens Chinese without encoding issues.
     */
    suspend fun createExcelSpreadsheet(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<GeneratedDocument> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(30)
            val fileName = "${cleanTitle}_$timestamp.csv"
            val file = File(getDocumentsDir(context), fileName)

            FileOutputStream(file).use { out ->
                // Write UTF-8 BOM (\uFEFF)
                out.write(0xEF)
                out.write(0xBB)
                out.write(0xBF)

                // Write Header Row
                val headerLine = headers.joinToString(",") { escapeCsv(it) } + "\n"
                out.write(headerLine.toByteArray(StandardCharsets.UTF_8))

                // Write Data Rows
                for (row in rows) {
                    val rowLine = row.joinToString(",") { escapeCsv(it) } + "\n"
                    out.write(rowLine.toByteArray(StandardCharsets.UTF_8))
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            Result.success(
                GeneratedDocument(
                    file = file,
                    uri = uri,
                    title = title,
                    fileName = fileName,
                    fileSizeBytes = file.length(),
                    formattedSize = formatFileSize(file.length()),
                    mimeType = "text/csv",
                    docType = DocumentType.EXCEL,
                    pageOrRowCount = rows.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Excel document", e)
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Open document with external viewer app.
     */
    fun openDocument(context: Context, docUri: Uri, mimeType: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(docUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "No app available to open document", e)
            false
        }
    }

    /**
     * Share document via Android Sharesheet.
     */
    fun shareDocument(context: Context, docUri: Uri, mimeType: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, docUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "分享文档: $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share document", e)
        }
    }

    /**
     * Save generated document directly to system public Downloads directory.
     */
    suspend fun saveToDownloads(context: Context, doc: GeneratedDocument): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, doc.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, doc.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DreamAI")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("无法创建下载文件项"))

                resolver.openOutputStream(targetUri)?.use { out ->
                    doc.file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)

                Result.success(targetUri)
            } else {
                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DreamAI")
                if (!publicDir.exists()) publicDir.mkdirs()
                val targetFile = File(publicDir, doc.fileName)
                doc.file.copyTo(targetFile, overwrite = true)
                Result.success(Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to Downloads", e)
            Result.failure(e)
        }
    }
}
