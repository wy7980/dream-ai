package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgnesCyan
import com.example.ui.theme.AgnesEmerald
import com.example.ui.theme.AgnesViolet
import com.example.ui.theme.AppCardBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppDivider
import com.example.ui.theme.AppSubtleBg
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class ListItem(val prefix: String, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

/**
 * Parses markdown text into structural blocks (Headings, Code, Lists, Quotes, Paragraphs).
 */
fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Multi-line Code block ```lang ... ```
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++ // skip closing ```
            continue
        }

        // 2. Horizontal divider --- or ***
        if (trimmed.matches(Regex("^(---+|\\*\\*\\*+|___+)$"))) {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 3. Headings #, ##, ###
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(6)
            val text = trimmed.removePrefix("#".repeat(level)).trim()
            blocks.add(MarkdownBlock.Heading(level, text))
            i++
            continue
        }

        // 4. Quotes >
        if (trimmed.startsWith(">")) {
            val quoteText = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.Quote(quoteText))
            i++
            continue
        }

        // 5. List items: -, *, +, 1., 2. etc.
        val listRegex = Regex("^([-*+•]|\\d+\\.)\\s+(.*)")
        val listMatch = listRegex.find(trimmed)
        if (listMatch != null) {
            val prefix = listMatch.groupValues[1]
            val text = listMatch.groupValues[2]
            blocks.add(MarkdownBlock.ListItem(prefix, text))
            i++
            continue
        }

        // 6. Empty line
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 7. Normal paragraph line
        blocks.add(MarkdownBlock.Paragraph(line))
        i++
    }
    return blocks
}

/**
 * Formats inline Markdown syntax (**bold**, *italic*, `code`, ~~strike~~, [links])
 */
@Composable
fun renderInlineMarkdown(text: String, textColor: Color = AppTextPrimary): AnnotatedString {
    val subtleBg = AppSubtleBg
    return buildAnnotatedString {
        val pattern = Regex("(\\*\\*.*?\\*\\*|__.*?__|`.*?`|~~.*?~~|\\[.*?\\]\\(.*?>?\\)|\\*.*?\\*|_.*?_)")
        var lastIndex = 0
        for (match in pattern.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            val value = match.value
            when {
                (value.startsWith("**") && value.endsWith("**") && value.length >= 4) -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                        append(value.substring(2, value.length - 2))
                    }
                }
                (value.startsWith("__") && value.endsWith("__") && value.length >= 4) -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                        append(value.substring(2, value.length - 2))
                    }
                }
                (value.startsWith("`") && value.endsWith("`") && value.length >= 2) -> {
                    val codeContent = value.substring(1, value.length - 1)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = subtleBg,
                            color = AgnesCyan,
                            fontSize = 11.5.sp
                        )
                    ) {
                        append(" $codeContent ")
                    }
                }
                (value.startsWith("~~") && value.endsWith("~~") && value.length >= 4) -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = textColor.copy(alpha = 0.7f))) {
                        append(value.substring(2, value.length - 2))
                    }
                }
                (value.startsWith("*") && value.endsWith("*") && value.length >= 2) -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                        append(value.substring(1, value.length - 1))
                    }
                }
                (value.startsWith("_") && value.endsWith("_") && value.length >= 2) -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                        append(value.substring(1, value.length - 1))
                    }
                }
                (value.startsWith("[") && value.contains("](")) -> {
                    val label = value.substringAfter("[").substringBefore("]")
                    withStyle(SpanStyle(color = AgnesCyan, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                        append(label)
                    }
                }
                else -> {
                    append(value)
                }
            }
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

/**
 * Full Jetpack Compose Markdown Renderer component
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 19.sp
) {
    val context = LocalContext.current
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingFontSize = when (block.level) {
                        1 -> 16.sp
                        2 -> 15.sp
                        else -> 14.sp
                    }
                    Text(
                        text = renderInlineMarkdown(block.text, textColor),
                        fontSize = headingFontSize,
                        fontWeight = FontWeight.Bold,
                        color = AgnesCyan,
                        lineHeight = (headingFontSize.value + 4).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.code,
                        context = context
                    )
                }

                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppSubtleBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(AgnesCyan, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = renderInlineMarkdown(block.text, textColor),
                            fontSize = fontSize,
                            color = textColor.copy(alpha = 0.9f),
                            lineHeight = lineHeight
                        )
                    }
                }

                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (block.prefix.matches(Regex("\\d+\\."))) block.prefix else "•",
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = AgnesCyan,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            text = renderInlineMarkdown(block.text, textColor),
                            fontSize = fontSize,
                            color = textColor,
                            lineHeight = lineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderInlineMarkdown(block.text, textColor),
                        fontSize = fontSize,
                        color = textColor,
                        lineHeight = lineHeight
                    )
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = AppDivider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    context: Context
) {
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppCardBg,
        border = BorderStroke(1.dp, AppCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSubtleBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AgnesCyan,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Code", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "代码已复制", Toast.LENGTH_SHORT).show()
                        isCopied = true
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (isCopied) AgnesEmerald else AppTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "已复制" else "复制",
                            fontSize = 10.sp,
                            color = if (isCopied) AgnesEmerald else AppTextSecondary
                        )
                    }
                }
            }

            // Code content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = code,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AppTextPrimary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
