package com.example.ui.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock
    data class BulletList(val items: List<String>) : MarkdownBlock
    data class NumberedList(val items: List<Pair<String, String>>) : MarkdownBlock
    data class Image(val altText: String, val url: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    object HorizontalRule : MarkdownBlock
}

object MarkdownFormatter {

    private fun unescapeMarkdown(text: String): String {
        return text.replace("\\*", "*")
            .replace("\\_", "_")
            .replace("\\~", "~")
            .replace("\\`", "`")
            .replace("\\[", "[")
            .replace("\\]", "]")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    /**
     * Formats inline markdown tokens (bold, italics, strikethrough, inline code).
     */
    fun formatMarkdown(rawText: String, primaryColor: Color? = null): AnnotatedString {
        // Preprocess: convert <br> tags to standard newlines
        val text = rawText.replace(Regex("(?i)<br\\s*/?>"), "\n")
        
        return buildAnnotatedString {
            val pattern = Regex(
                "(?<!\\\\\\\\)(\\*\\*\\*(.+?)(?<!\\\\\\\\)\\*\\*\\*|(?<!\\\\\\\\)___(.+?)(?<!\\\\\\\\)___|(?<!\\\\\\\\)\\*\\*(.+?)(?<!\\\\\\\\)\\*\\*|(?<!\\\\\\\\)__(.+?)(?<!\\\\\\\\)__|(?<!\\\\\\\\)\\*(.+?)(?<!\\\\\\\\)\\*|(?<!\\\\\\\\)_(.+?)(?<!\\\\\\\\)_|(?<!\\\\\\\\)~~(.+?)(?<!\\\\\\\\)~~|(?<!\\\\\\\\)`([^`]+)(?<!\\\\\\\\)`)",
                RegexOption.DOT_MATCHES_ALL
            )
            var lastIndex = 0

            val matches = pattern.findAll(text)
            for (match in matches) {
                if (match.range.first > lastIndex) {
                    append(unescapeMarkdown(text.substring(lastIndex, match.range.first)))
                }

                val fullMatch = match.value
                when {
                    // Bold & Italic
                    (fullMatch.startsWith("***") && fullMatch.endsWith("***") && fullMatch.length >= 6) ||
                    (fullMatch.startsWith("___") && fullMatch.endsWith("___") && fullMatch.length >= 6) -> {
                        val inner = fullMatch.substring(3, fullMatch.length - 3)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(unescapeMarkdown(inner))
                        }
                    }

                    // Bold
                    (fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length >= 4) ||
                    (fullMatch.startsWith("__") && fullMatch.endsWith("__") && fullMatch.length >= 4) -> {
                        val inner = fullMatch.substring(2, fullMatch.length - 2)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(unescapeMarkdown(inner))
                        }
                    }

                    // Strikethrough
                    fullMatch.startsWith("~~") && fullMatch.endsWith("~~") && fullMatch.length >= 4 -> {
                        val inner = fullMatch.substring(2, fullMatch.length - 2)
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(unescapeMarkdown(inner))
                        }
                    }

                    // Italic
                    (fullMatch.startsWith("*") && fullMatch.endsWith("*") && fullMatch.length >= 2) ||
                    (fullMatch.startsWith("_") && fullMatch.endsWith("_") && fullMatch.length >= 2) -> {
                        val inner = fullMatch.substring(1, fullMatch.length - 1)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(unescapeMarkdown(inner))
                        }
                    }

                    // Inline code
                    fullMatch.startsWith("`") && fullMatch.endsWith("`") && fullMatch.length >= 2 -> {
                        val inner = fullMatch.substring(1, fullMatch.length - 1)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = primaryColor?.copy(alpha = 0.15f) ?: Color.Gray.copy(alpha = 0.2f)
                            )
                        ) {
                            append(" ")
                            append(unescapeMarkdown(inner))
                            append(" ")
                        }
                    }

                    else -> {
                        append(unescapeMarkdown(fullMatch))
                    }
                }

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.length) {
                append(unescapeMarkdown(text.substring(lastIndex)))
            }
        }
    }

    /**
     * Parses raw markdown into structured blocks (Tables, Headings, Blockquotes, Lists, Code blocks, Paragraphs).
     */
    fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = rawText.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Skip empty lines
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 2. Fenced code block (```)
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim().takeIf { it.isNotEmpty() }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size && lines[i].trim().startsWith("```")) {
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
                continue
            }

            // 3. Table: current line has '|' and next line is separator
            if (trimmed.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1].trim())) {
                val headers = parseTableRow(trimmed)
                val rows = mutableListOf<List<String>>()
                i += 2 // skip header and separator
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (rowLine.isEmpty() || !rowLine.contains("|") || rowLine.startsWith("#") || rowLine.startsWith("```")) {
                        break
                    }
                    rows.add(parseTableRow(rowLine))
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
                continue
            }

            // 4. Standalone Markdown Image or Image URL: ![alt](url) or http(s)://...png/jpg/webp/gif
            val imagePattern = Regex("^!\\[([^\\]]*)\\]\\(\\s*(https?://\\S+|data:image/[^)\\s]+)(?:\\s+[\"'][^\"']*[\"'])?\\s*\\)$")
            val imageMatch = imagePattern.find(trimmed)
            if (imageMatch != null) {
                val alt = imageMatch.groupValues[1]
                val url = imageMatch.groupValues[2]
                blocks.add(MarkdownBlock.Image(alt, url))
                i++
                continue
            }

            // Also check for raw direct image link on a single line
            val rawImagePattern = Regex("^(https?://\\S+\\.(?:png|jpg|jpeg|webp|gif|svg)(?:\\?\\S*)?)$", RegexOption.IGNORE_CASE)
            val rawImageMatch = rawImagePattern.find(trimmed)
            if (rawImageMatch != null) {
                val url = rawImageMatch.groupValues[1]
                blocks.add(MarkdownBlock.Image("Generated Image", url))
                i++
                continue
            }

            // 5. Horizontal rule (---, ***, ___)
            if (trimmed.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$"))) {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // 6. Headings (#, ##, ###, ####, #####, ######)
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmed)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].trim()
                blocks.add(MarkdownBlock.Heading(level, text))
                i++
                continue
            }

            // 7. Blockquote (lines starting with >)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size) {
                    val qLine = lines[i].trim()
                    if (qLine.startsWith(">")) {
                        quoteLines.add(qLine.removePrefix(">").trim())
                        i++
                    } else if (qLine.isEmpty()) {
                        break
                    } else {
                        quoteLines.add(qLine)
                        i++
                    }
                }
                blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
                continue
            }

            // 8. Bullet List (- , * , + , • )
            val bulletMatch = Regex("^([-*+•])\\s+(.+)$").find(trimmed)
            if (bulletMatch != null) {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val bLine = lines[i].trim()
                    val bMatch = Regex("^([-*+•])\\s+(.+)$").find(bLine)
                    if (bMatch != null) {
                        items.add(bMatch.groupValues[2].trim())
                        i++
                    } else if (bLine.isNotEmpty() && items.isNotEmpty() && !bLine.startsWith("#") && !bLine.startsWith(">") && !bLine.contains("|")) {
                        val last = items.removeAt(items.size - 1)
                        items.add("$last $bLine")
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.BulletList(items))
                continue
            }

            // 9. Numbered List (1. , 2. )
            val numMatch = Regex("^(\\d+)[.)]\\s+(.+)$").find(trimmed)
            if (numMatch != null) {
                val items = mutableListOf<Pair<String, String>>()
                while (i < lines.size) {
                    val nLine = lines[i].trim()
                    val nMatch = Regex("^(\\d+)[.)]\\s+(.+)$").find(nLine)
                    if (nMatch != null) {
                        items.add(Pair(nMatch.groupValues[1], nMatch.groupValues[2].trim()))
                        i++
                    } else if (nLine.isNotEmpty() && items.isNotEmpty() && !nLine.startsWith("#") && !nLine.startsWith(">") && !nLine.contains("|")) {
                        val last = items.removeAt(items.size - 1)
                        items.add(Pair(last.first, "${last.second} $nLine"))
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.NumberedList(items))
                continue
            }

            // 10. Standard Paragraph (with embedded image checking)
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val pLine = lines[i].trim()
                if (pLine.isEmpty() ||
                    pLine.startsWith("#") ||
                    pLine.startsWith("```") ||
                    pLine.startsWith(">") ||
                    imagePattern.matches(pLine) ||
                    rawImagePattern.matches(pLine) ||
                    Regex("^([-*+•])\\s+").containsMatchIn(pLine) ||
                    Regex("^(\\d+)[.)]\\s+").containsMatchIn(pLine) ||
                    (pLine.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1].trim()))
                ) {
                    break
                }

                // If paragraph line itself contains an inline ![alt](url), break so it can be handled
                if (pLine.contains("![") && pLine.contains("](") && imagePattern.matches(pLine)) {
                    break
                }

                paragraphLines.add(lines[i])
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                val text = paragraphLines.joinToString("\n")
                val imgRegex = Regex("!\\[([^\\]]*)\\]\\(\\s*(https?://\\S+|data:image/[^)\\s]+)(?:\\s+[\"'][^\"']*[\"'])?\\s*\\)")
                var cursor = 0
                val matches = imgRegex.findAll(text).toList()
                if (matches.isEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(text))
                } else {
                    for (m in matches) {
                        val before = text.substring(cursor, m.range.first).trim()
                        if (before.isNotEmpty()) {
                            blocks.add(MarkdownBlock.Paragraph(before))
                        }
                        val alt = m.groupValues[1]
                        val url = m.groupValues[2]
                        blocks.add(MarkdownBlock.Image(alt, url))
                        cursor = m.range.last + 1
                    }
                    if (cursor < text.length) {
                        val after = text.substring(cursor).trim()
                        if (after.isNotEmpty()) {
                            blocks.add(MarkdownBlock.Paragraph(after))
                        }
                    }
                }
            }
        }

        return blocks
    }

    private fun isTableSeparator(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.contains("-") || !trimmed.contains("|")) return false
        return trimmed.all { it == '|' || it == '-' || it == ':' || it.isWhitespace() }
    }

    private fun parseTableRow(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
        if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length - 1)
        return trimmed.split("|").map { it.trim() }
    }
}

/**
 * Rich Composable for rendering full Markdown text with Tables, Headings,
 * Blockquotes, Lists, Code blocks, and styled text.
 */
@Composable
fun MarkdownContent(
    content: String,
    textColor: Color,
    primaryColor: Color,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: ((String) -> Unit)? = null
) {
    val blocks = remember(content) {
        MarkdownFormatter.parseMarkdownBlocks(content)
    }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        3 -> 16.sp
                        else -> 15.sp
                    }
                    val topPadding = when (block.level) {
                        1 -> 10.dp
                        2 -> 8.dp
                        3 -> 6.dp
                        else -> 4.dp
                    }
                    Text(
                        text = MarkdownFormatter.formatMarkdown(block.text, primaryColor),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(top = topPadding, bottom = 3.dp)
                    )
                }

                is MarkdownBlock.Table -> {
                    MarkdownTable(
                        headers = block.headers,
                        rows = block.rows,
                        textColor = textColor,
                        primaryColor = primaryColor,
                        isUser = isUser
                    )
                }

                is MarkdownBlock.Blockquote -> {
                    MarkdownBlockquote(
                        text = block.text,
                        textColor = textColor,
                        primaryColor = primaryColor,
                        isUser = isUser
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    MarkdownCodeBlock(
                        code = block.code,
                        language = block.language,
                        textColor = textColor,
                        primaryColor = primaryColor,
                        isUser = isUser
                    )
                }

                is MarkdownBlock.BulletList -> {
                    MarkdownBulletList(
                        items = block.items,
                        textColor = textColor,
                        primaryColor = primaryColor
                    )
                }

                is MarkdownBlock.NumberedList -> {
                    MarkdownNumberedList(
                        items = block.items,
                        textColor = textColor,
                        primaryColor = primaryColor
                    )
                }

                is MarkdownBlock.Image -> {
                    MarkdownImageBlock(
                        altText = block.altText,
                        imageUrl = block.url,
                        isUser = isUser,
                        primaryColor = primaryColor,
                        onImageClick = onImageClick
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = MarkdownFormatter.formatMarkdown(block.text, primaryColor),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp
                        ),
                        color = textColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = if (isUser) textColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    textColor: Color,
    primaryColor: Color,
    isUser: Boolean
) {
    val scrollState = rememberScrollState()
    val borderColor = if (isUser) {
        textColor.copy(alpha = 0.25f)
    } else {
        primaryColor.copy(alpha = 0.35f)
    }
    val headerBgColor = if (isUser) {
        textColor.copy(alpha = 0.15f)
    } else {
        primaryColor.copy(alpha = 0.15f)
    }
    val alternateRowBgColor = if (isUser) {
        textColor.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val columnWidths = remember(headers, rows) {
        headers.indices.map { colIdx ->
            val headerLen = headers.getOrNull(colIdx)?.length ?: 0
            val maxCellLen = rows.maxOfOrNull { it.getOrNull(colIdx)?.length ?: 0 } ?: 0
            val maxLen = maxOf(headerLen, maxCellLen)
            when {
                maxLen < 15 -> 100.dp
                maxLen < 30 -> 140.dp
                maxLen < 50 -> 200.dp
                maxLen < 80 -> 260.dp
                else -> 320.dp
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .padding(vertical = 8.dp)
            // Removed fillMaxWidth() to fix the white gap on narrow tables
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(headerBgColor)
                        .height(IntrinsicSize.Min)
                ) {
                    headers.forEachIndexed { colIdx, header ->
                        Box(
                            modifier = Modifier
                                .width(columnWidths[colIdx])
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = MarkdownFormatter.formatMarkdown(header, primaryColor),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) textColor else primaryColor
                                )
                            )
                        }
                        if (colIdx < headers.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(borderColor)
                            )
                        }
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp)

                // Data Rows
                rows.forEachIndexed { rowIndex, rowCells ->
                    val rowBg = if (rowIndex % 2 == 1) alternateRowBgColor else Color.Transparent
                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .height(IntrinsicSize.Min)
                    ) {
                        headers.indices.forEach { colIdx ->
                            val cellText = rowCells.getOrNull(colIdx) ?: ""
                            Box(
                                modifier = Modifier
                                    .width(columnWidths[colIdx])
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = MarkdownFormatter.formatMarkdown(cellText, primaryColor),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColor,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                            if (colIdx < headers.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(borderColor)
                                )
                            }
                        }
                    }
                    if (rowIndex < rows.size - 1) {
                        HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownBlockquote(
    text: String,
    textColor: Color,
    primaryColor: Color,
    isUser: Boolean
) {
    val barColor = if (isUser) textColor.copy(alpha = 0.8f) else primaryColor
    val bgColor = if (isUser) textColor.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(bgColor)
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MarkdownFormatter.formatMarkdown(text, primaryColor),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = textColor.copy(alpha = 0.9f)
            ),
            modifier = Modifier.padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
        )
    }
}

@Composable
fun MarkdownCodeBlock(
    code: String,
    language: String?,
    textColor: Color,
    primaryColor: Color,
    isUser: Boolean
) {
    val hScrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isUser) Color.Black.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (isUser) textColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isUser) textColor.copy(alpha = 0.12f) else primaryColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (if (!language.isNullOrBlank()) language else "code").uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) textColor else primaryColor
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(code))
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (ignored: Exception) {}
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        modifier = Modifier.size(13.dp),
                        tint = if (isUser) textColor.copy(alpha = 0.85f) else primaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isUser) textColor.copy(alpha = 0.85f) else primaryColor
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScrollState)
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = textColor,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun MarkdownBulletList(
    items: List<String>,
    textColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    ),
                    modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                )
                Text(
                    text = MarkdownFormatter.formatMarkdown(item, primaryColor),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MarkdownNumberedList(
    items: List<Pair<String, String>>,
    textColor: Color,
    primaryColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        items.forEach { (number, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$number. ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    ),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )
                Text(
                    text = MarkdownFormatter.formatMarkdown(text, primaryColor),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MarkdownImageBlock(
    altText: String,
    imageUrl: String,
    isUser: Boolean,
    primaryColor: Color,
    onImageClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                1.dp,
                if (isUser) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = altText.ifEmpty { "Generated Image" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (onImageClick != null) {
                                onImageClick(imageUrl)
                            }
                        },
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp),
                                color = primaryColor
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Unable to load image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )

                // Download floating action button on top right of the image
                FilledTonalIconButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            coroutineScope.launch {
                                ImageDownloadHelper.downloadAndSaveImage(context, imageUrl)
                                isDownloading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download Image",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (altText.isNotBlank()) {
            Text(
                text = altText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

