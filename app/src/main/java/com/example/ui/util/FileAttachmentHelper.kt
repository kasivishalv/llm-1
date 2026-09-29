package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

enum class AttachmentCategory(val title: String) {
    IMAGE("Image"),
    DOCUMENT("Document"),
    SPREADSHEET("Spreadsheet"),
    PRESENTATION("Presentation"),
    PDF("PDF Document"),
    TEXT("Text File")
}

data class ProcessedAttachment(
    val fileName: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val category: AttachmentCategory,
    val localPreviewUri: String? = null,
    val base64Image: String? = null,
    val extractedText: String? = null,
    val previewSnippet: String? = null
)

object FileAttachmentHelper {

    suspend fun processAttachment(context: Context, uri: Uri): ProcessedAttachment = withContext(Dispatchers.IO) {
        val fileName = getFileName(context, uri) ?: "attachment"
        val reportedSize = getFileSize(context, uri)

        // 1. Copy URI content to a local temp file safely
        val tempFile = copyUriToTempFile(context, uri, fileName)
        val actualFile = tempFile ?: try {
            File(context.cacheDir, "att_empty_${System.currentTimeMillis()}").apply {
                createNewFile()
            }
        } catch (e: Exception) {
            null
        }

        val fileSize = if (actualFile != null && actualFile.exists()) actualFile.length() else reportedSize
        val ext = detectExtension(fileName, context, uri, actualFile).lowercase()

        // Handle empty file case gracefully without error
        if (actualFile == null || !actualFile.exists() || fileSize == 0L) {
            return@withContext ProcessedAttachment(
                fileName = fileName,
                fileExtension = ext.ifBlank { "txt" },
                fileSizeBytes = 0L,
                category = AttachmentCategory.DOCUMENT,
                extractedText = "The attached file `$fileName` is empty (0 bytes).",
                previewSnippet = "Empty file (0 bytes)"
            )
        }

        try {
            when (ext) {
                // Images and Graphics
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "ico", "heic", "heif", "tiff", "tif" -> {
                    processImageFile(context, actualFile, fileName, ext, fileSize)
                }
                "svg" -> {
                    processSvgFile(actualFile, fileName, fileSize)
                }

                // PDF Documents
                "pdf" -> {
                    processPdfFile(context, actualFile, fileName, fileSize)
                }

                // Microsoft Word
                "docx", "dotx" -> {
                    processDocxFile(actualFile, fileName, fileSize)
                }
                "doc", "dot" -> {
                    processLegacyWordFile(actualFile, fileName, fileSize)
                }

                // OpenDocument
                "odt", "fodt" -> {
                    processOdtFile(actualFile, fileName, fileSize)
                }
                "ods", "fods" -> {
                    processOdsFile(actualFile, fileName, fileSize)
                }
                "odp", "fodp" -> {
                    processOdpFile(actualFile, fileName, fileSize)
                }

                // Microsoft Excel & Tabular
                "xlsx", "xlsm", "xltx" -> {
                    processXlsxFile(actualFile, fileName, fileSize)
                }
                "xls", "xlt" -> {
                    processLegacyExcelFile(actualFile, fileName, fileSize)
                }
                "csv", "tsv" -> {
                    processCsvFile(actualFile, fileName, ext, fileSize)
                }

                // Microsoft PowerPoint
                "pptx", "ppsx", "pptm" -> {
                    processPptxFile(actualFile, fileName, fileSize)
                }
                "ppt", "pps" -> {
                    processLegacyPptFile(actualFile, fileName, fileSize)
                }

                // Rich Text
                "rtf" -> {
                    processRtfFile(actualFile, fileName, fileSize)
                }

                // Archives
                "zip", "jar", "apk", "aar", "tar", "gz", "7z", "rar" -> {
                    processArchiveFile(actualFile, fileName, ext, fileSize)
                }

                // Text, Markup, Configuration, Source Code
                "txt", "text", "md", "markdown", "json", "xml", "html", "htm", "yaml", "yml",
                "ini", "conf", "cfg", "properties", "log", "kt", "java", "py", "js", "ts",
                "cpp", "c", "h", "cs", "sh", "bash", "sql", "css", "env", "gradle", "toml" -> {
                    processTextFile(actualFile, fileName, ext, fileSize)
                }

                // Generic Binary / Media / Unknown
                else -> {
                    processGenericFile(actualFile, fileName, ext, fileSize)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            // Always succeed with fallback text/metadata
            processGenericFile(actualFile, fileName, ext, fileSize)
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, safeName: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "attachments").apply { if (!exists()) mkdirs() }
            val cleanName = safeName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
            val tempFile = File(cacheDir, "att_${System.currentTimeMillis()}_$cleanName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists()) tempFile else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            name = it.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/')
        }
        return name
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.SIZE)
                        if (index >= 0) {
                            size = it.getLong(index)
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return size
    }

    private fun detectExtension(fileName: String, context: Context, uri: Uri, file: File?): String {
        // 1. Extension from filename
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot >= 0 && lastDot < fileName.length - 1) {
            val ext = fileName.substring(lastDot + 1).lowercase()
            if (ext.isNotEmpty()) return ext
        }

        // 2. MIME type
        val mime = (try { context.contentResolver.getType(uri) } catch (e: Exception) { null }) ?: ""
        val mimeExt = when {
            mime.contains("pdf") -> "pdf"
            mime.contains("wordprocessingml") -> "docx"
            mime.contains("msword") -> "doc"
            mime.contains("spreadsheetml") -> "xlsx"
            mime.contains("ms-excel") -> "xls"
            mime.contains("presentationml") -> "pptx"
            mime.contains("ms-powerpoint") -> "ppt"
            mime.contains("opendocument.text") -> "odt"
            mime.contains("opendocument.spreadsheet") -> "ods"
            mime.contains("opendocument.presentation") -> "odp"
            mime.contains("rtf") -> "rtf"
            mime.contains("csv") -> "csv"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            mime.contains("svg") -> "svg"
            mime.contains("tiff") -> "tiff"
            mime.contains("zip") -> "zip"
            mime.contains("json") -> "json"
            mime.contains("html") -> "html"
            mime.contains("text/plain") -> "txt"
            else -> ""
        }
        if (mimeExt.isNotEmpty()) return mimeExt

        // 3. Inspect magic bytes if file exists
        if (file != null && file.exists() && file.length() >= 4) {
            try {
                val header = ByteArray(16)
                FileInputStream(file).use { it.read(header) }
                if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
                    return "pdf"
                }
                if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                    // ZIP container - check contents
                    return detectZipContentType(file)
                }
                if (header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() && header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()) {
                    return "doc" // OLE2 container
                }
                if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
                    return "jpg"
                }
                if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
                    return "png"
                }
                if (header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte()) {
                    return "gif"
                }
                if (header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte()) {
                    return "webp"
                }
                if (header[0] == 0x7B.toByte() && header[1] == 0x5C.toByte() && header[2] == 0x72.toByte() && header[3] == 0x74.toByte()) {
                    return "rtf"
                }
            } catch (e: Exception) {}
        }

        return "txt"
    }

    private fun detectZipContentType(file: File): String {
        return try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                var foundType = "zip"
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("word/")) return "docx"
                    if (name.startsWith("xl/")) return "xlsx"
                    if (name.startsWith("ppt/")) return "pptx"
                    if (name == "content.xml") return "odt"
                    entry = zip.nextEntry
                }
                foundType
            }
        } catch (e: Exception) {
            "zip"
        }
    }

    // --- Format Handlers ---

    private fun processImageFile(
        context: Context,
        file: File,
        fileName: String,
        ext: String,
        fileSize: Long
    ): ProcessedAttachment {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val origW = options.outWidth
            val origH = options.outHeight

            if (origW > 0 && origH > 0) {
                val maxDim = 1024
                var sample = 1
                while ((origW / sample) > maxDim * 2 || (origH / sample) > maxDim * 2) {
                    sample *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val sampled = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                if (sampled != null) {
                    val finalBitmap = if (sampled.width > maxDim || sampled.height > maxDim) {
                        val ratio = sampled.width.toFloat() / sampled.height.toFloat()
                        val targetW = if (ratio > 1f) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
                        val targetH = if (ratio > 1f) (maxDim / ratio).toInt().coerceAtLeast(1) else maxDim
                        val scaled = Bitmap.createScaledBitmap(sampled, targetW, targetH, true)
                        if (scaled != sampled) sampled.recycle()
                        scaled
                    } else sampled

                    val bos = ByteArrayOutputStream()
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                    val bytes = bos.toByteArray()
                    finalBitmap.recycle()

                    val imagesDir = File(context.filesDir, "chat_images").apply { if (!exists()) mkdirs() }
                    val persistentFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(persistentFile).use { it.write(bytes) }

                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return ProcessedAttachment(
                        fileName = fileName,
                        fileExtension = ext,
                        fileSizeBytes = fileSize,
                        category = AttachmentCategory.IMAGE,
                        localPreviewUri = persistentFile.absolutePath,
                        base64Image = base64,
                        extractedText = "Attached Image: $fileName (${origW}x${origH} px)",
                        previewSnippet = "Image: $fileName (${origW}x${origH})"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = ext,
            fileSizeBytes = fileSize,
            category = AttachmentCategory.IMAGE,
            extractedText = "Attached Image: $fileName (${formatFileSize(fileSize)})",
            previewSnippet = "Image: $fileName"
        )
    }

    private fun processSvgFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val rawXml = try { file.readText(Charsets.UTF_8) } catch (e: Exception) { "" }
        val snippet = if (rawXml.length > 300) rawXml.take(300) + "..." else rawXml
        val content = if (rawXml.isNotBlank()) "```xml\n$rawXml\n```" else "SVG Graphic: $fileName"

        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "svg",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.IMAGE,
            extractedText = content,
            previewSnippet = "SVG: $snippet"
        )
    }

    private fun processPdfFile(
        context: Context,
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        var previewPath: String? = null
        var base64Preview: String? = null
        var pageCount = 0

        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount
            if (pageCount > 0) {
                val page = renderer.openPage(0)
                val w = (page.width * 1.5).toInt().coerceIn(300, 1200)
                val h = (page.height * 1.5).toInt().coerceIn(300, 1800)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                val bytes = bos.toByteArray()
                bitmap.recycle()

                val imagesDir = File(context.filesDir, "chat_images").apply { if (!exists()) mkdirs() }
                val persistentFile = File(imagesDir, "pdf_${System.currentTimeMillis()}.jpg")
                FileOutputStream(persistentFile).use { it.write(bytes) }

                previewPath = persistentFile.absolutePath
                base64Preview = Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val extractedText = extractTextFromPdfStream(file)
        val body = buildString {
            appendLine("### PDF Document: `$fileName` ($pageCount pages)")
            if (extractedText.isNotBlank()) {
                appendLine()
                appendLine(extractedText)
            } else {
                appendLine()
                appendLine("*(Visual PDF with $pageCount pages. First page snapshot attached for visual and textual inspection.)*")
            }
        }.trim()

        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "pdf",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.PDF,
            localPreviewUri = previewPath,
            base64Image = base64Preview,
            extractedText = body,
            previewSnippet = if (extractedText.isNotBlank()) extractedText.take(200) + "..." else "PDF ($pageCount pages)"
        )
    }

    private fun extractTextFromPdfStream(file: File): String {
        return try {
            val bytes = file.readBytes()
            val content = String(bytes, Charsets.ISO_8859_1)
            val sb = StringBuilder()

            // Extract TJ and Tj text blocks
            val btPattern = Regex("(?s)BT(.*?)ET")
            for (match in btPattern.findAll(content)) {
                val block = match.groupValues[1]
                val tjPattern = Regex("\\((.*?)\\)\\s*Tj")
                for (tj in tjPattern.findAll(block)) {
                    val str = tj.groupValues[1]
                        .replace("\\(", "(")
                        .replace("\\)", ")")
                        .replace("\\\\", "\\")
                    sb.append(str).append(" ")
                }
                val tjArrayPattern = Regex("\\[(.*?)\\]\\s*TJ")
                for (tjArr in tjArrayPattern.findAll(block)) {
                    val arrStr = tjArr.groupValues[1]
                    for (p in Regex("\\((.*?)\\)").findAll(arrStr)) {
                        sb.append(p.groupValues[1])
                    }
                    sb.append(" ")
                }
                sb.append("\n")
            }

            val result = sb.toString().trim()
            if (result.isNotBlank()) {
                if (result.length > 60_000) result.take(60_000) + "\n... [Truncated]" else result
            } else {
                extractReadableStrings(file).take(10_000)
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun processDocxFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        var docXml: String? = null
        var footnotesXml: String? = null
        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        docXml = zip.bufferedReader(Charsets.UTF_8).readText()
                    } else if (entry.name == "word/footnotes.xml") {
                        footnotesXml = zip.bufferedReader(Charsets.UTF_8).readText()
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val extracted = if (!docXml.isNullOrBlank()) {
            extractTextFromOpenXml(docXml!!)
        } else {
            extractReadableStrings(file)
        }

        val content = buildString {
            appendLine("### Microsoft Word Document: `$fileName`\n")
            appendLine(extracted)
            if (!footnotesXml.isNullOrBlank()) {
                val fnText = extractTextFromOpenXml(footnotesXml!!)
                if (fnText.isNotBlank()) {
                    appendLine("\n**Footnotes:**\n$fnText")
                }
            }
        }.trim()

        val truncated = if (content.length > 80_000) content.take(80_000) + "\n\n... [Truncated]" else content
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "docx",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.DOCUMENT,
            extractedText = truncated,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processLegacyWordFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val extracted = extractReadableStrings(file)
        val content = "### Microsoft Word (Legacy .doc): `$fileName`\n\n$extracted"
        val truncated = if (content.length > 80_000) content.take(80_000) + "\n... [Truncated]" else content
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "doc",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.DOCUMENT,
            extractedText = truncated,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processOdtFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        var contentXml: String? = null
        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "content.xml") {
                        contentXml = zip.bufferedReader(Charsets.UTF_8).readText()
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {}

        val extracted = if (!contentXml.isNullOrBlank()) {
            extractTextFromOdfXml(contentXml!!)
        } else {
            extractReadableStrings(file)
        }

        val content = "### OpenDocument Text: `$fileName`\n\n$extracted"
        val truncated = if (content.length > 80_000) content.take(80_000) + "\n... [Truncated]" else content
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "odt",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.DOCUMENT,
            extractedText = truncated,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processOdsFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        var contentXml: String? = null
        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "content.xml") {
                        contentXml = zip.bufferedReader(Charsets.UTF_8).readText()
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {}

        val extracted = if (!contentXml.isNullOrBlank()) {
            convertOdsXmlToMarkdown(contentXml!!, fileName)
        } else {
            extractReadableStrings(file)
        }

        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "ods",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.SPREADSHEET,
            extractedText = extracted,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processOdpFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        var contentXml: String? = null
        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "content.xml") {
                        contentXml = zip.bufferedReader(Charsets.UTF_8).readText()
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {}

        val extracted = if (!contentXml.isNullOrBlank()) {
            extractTextFromOdfXml(contentXml!!)
        } else {
            extractReadableStrings(file)
        }

        val content = "### OpenDocument Presentation: `$fileName`\n\n$extracted"
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "odp",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.PRESENTATION,
            extractedText = content,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processXlsxFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val sharedStrings = mutableListOf<String>()
        val sheetXmlMap = sortedMapOf<String, String>()

        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml") {
                        val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                        sharedStrings.addAll(extractSharedStrings(xml))
                    } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        val sheetName = name.substringAfterLast('/').removeSuffix(".xml")
                        sheetXmlMap[sheetName] = zip.bufferedReader(Charsets.UTF_8).readText()
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sb = StringBuilder()
        sb.appendLine("### Excel Spreadsheet: `$fileName`\n")

        if (sheetXmlMap.isEmpty()) {
            val strings = extractReadableStrings(file)
            sb.appendLine(strings.ifBlank { "Spreadsheet parsed (no sheet data found)." })
        } else {
            sheetXmlMap.forEach { (sheetKey, xml) ->
                val sheetMarkdown = convertSheetXmlToMarkdown(xml, sharedStrings)
                sb.appendLine("#### ${sheetKey.replaceFirstChar { it.uppercase() }}:")
                sb.appendLine(sheetMarkdown)
                sb.appendLine()
            }
        }

        val full = sb.toString().trim()
        val truncated = if (full.length > 80_000) full.take(80_000) + "\n... [Truncated]" else full

        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "xlsx",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.SPREADSHEET,
            extractedText = truncated,
            previewSnippet = truncated.take(300) + "..."
        )
    }

    private fun processLegacyExcelFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val extracted = extractReadableStrings(file)
        val content = "### Microsoft Excel (Legacy .xls): `$fileName`\n\n$extracted"
        val truncated = if (content.length > 80_000) content.take(80_000) + "\n... [Truncated]" else content
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "xls",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.SPREADSHEET,
            extractedText = truncated,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processCsvFile(
        file: File,
        fileName: String,
        ext: String,
        fileSize: Long
    ): ProcessedAttachment {
        val lines = try {
            file.readLines(Charsets.UTF_8)
        } catch (e: Exception) {
            try { file.readLines(Charsets.ISO_8859_1) } catch (e2: Exception) { emptyList() }
        }

        if (lines.isEmpty()) {
            return ProcessedAttachment(
                fileName = fileName,
                fileExtension = ext,
                fileSizeBytes = fileSize,
                category = AttachmentCategory.SPREADSHEET,
                extractedText = "Empty CSV table: `$fileName`",
                previewSnippet = "Empty spreadsheet"
            )
        }

        val delimiter = if (lines.first().contains("\t")) "\t" else if (lines.first().contains(";")) ";" else ","
        val maxRows = 150
        val sb = StringBuilder()
        sb.appendLine("### Tabular Data from `$fileName`\n")

        val tableLines = lines.take(maxRows)
        for (i in tableLines.indices) {
            val cols = parseCsvLine(tableLines[i], delimiter)
            sb.append("| ").append(cols.joinToString(" | ")).appendLine(" |")
            if (i == 0) {
                sb.append("| ").append(cols.joinToString(" | ") { "---" }).appendLine(" |")
            }
        }

        if (lines.size > maxRows) {
            sb.appendLine("\n*(Showing first $maxRows of ${lines.size} rows)*")
        }

        val result = sb.toString()
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = ext,
            fileSizeBytes = fileSize,
            category = AttachmentCategory.SPREADSHEET,
            extractedText = result,
            previewSnippet = result.take(300) + "..."
        )
    }

    private fun parseCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (!inQuotes && line.startsWith(delimiter, i)) {
                result.add(current.toString().trim())
                current.clear()
                i += delimiter.length - 1
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result.map { it.replace("|", "&#124;") }
    }

    private fun processPptxFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val slides = sortedMapOf<Int, String>()
        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val match = Regex("ppt/slides/slide([0-9]+)\\.xml").matchEntire(entry.name)
                    if (match != null) {
                        val slideNum = match.groupValues[1].toInt()
                        slides[slideNum] = zip.bufferedReader(Charsets.UTF_8).readText()
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sb = StringBuilder()
        sb.appendLine("### PowerPoint Presentation: `$fileName` (${slides.size} slides)\n")

        if (slides.isEmpty()) {
            val strings = extractReadableStrings(file)
            sb.appendLine(strings.ifBlank { "Presentation parsed (no slide text found)." })
        } else {
            slides.forEach { (num, xml) ->
                val slideText = extractTextFromOpenXml(xml)
                if (slideText.isNotBlank()) {
                    sb.appendLine("#### Slide $num:")
                    sb.appendLine(slideText)
                    sb.appendLine()
                }
            }
        }

        val full = sb.toString().trim()
        val truncated = if (full.length > 80_000) full.take(80_000) + "\n... [Truncated]" else full

        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "pptx",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.PRESENTATION,
            extractedText = truncated,
            previewSnippet = truncated.take(250) + "..."
        )
    }

    private fun processLegacyPptFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val extracted = extractReadableStrings(file)
        val content = "### Microsoft PowerPoint (Legacy .ppt): `$fileName`\n\n$extracted"
        val truncated = if (content.length > 80_000) content.take(80_000) + "\n... [Truncated]" else content
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "ppt",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.PRESENTATION,
            extractedText = truncated,
            previewSnippet = extracted.take(250) + "..."
        )
    }

    private fun processRtfFile(
        file: File,
        fileName: String,
        fileSize: Long
    ): ProcessedAttachment {
        val raw = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            try { file.readText(Charsets.ISO_8859_1) } catch (e2: Exception) { "" }
        }

        val plainText = extractTextFromRtf(raw)
        val truncated = if (plainText.length > 80_000) plainText.take(80_000) + "..." else plainText
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = "rtf",
            fileSizeBytes = fileSize,
            category = AttachmentCategory.DOCUMENT,
            extractedText = "### Rich Text Document: `$fileName`\n\n$truncated",
            previewSnippet = truncated.take(250) + "..."
        )
    }

    private fun extractTextFromRtf(rtf: String): String {
        var text = rtf.replace(Regex("(?s)\\{\\\\\\*?[a-zA-Z0-9]+.*?\\}"), "")
        text = text.replace(Regex("\\\\(par|line) ?"), "\n")
        text = text.replace(Regex("\\\\tab ?"), "\t")
        text = text.replace(Regex("\\\\'([0-9a-fA-F]{2})")) { match ->
            try { match.groupValues[1].toInt(16).toChar().toString() } catch (e: Exception) { "" }
        }
        text = text.replace(Regex("\\\\u([0-9]+)\\??")) { match ->
            try { match.groupValues[1].toInt().toChar().toString() } catch (e: Exception) { "" }
        }
        text = text.replace(Regex("\\\\[a-zA-Z]+-?[0-9]* ?"), "")
        text = text.replace("{", "").replace("}", "")
        return text.trim()
    }

    private fun processArchiveFile(
        file: File,
        fileName: String,
        ext: String,
        fileSize: Long
    ): ProcessedAttachment {
        val entries = mutableListOf<String>()
        var readmeContent: String? = null

        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                var count = 0
                while (entry != null && count < 200) {
                    val name = entry.name
                    val size = if (entry.size >= 0) " (${formatFileSize(entry.size)})" else ""
                    entries.add("- `${name}`$size")
                    if (readmeContent == null && (name.contains("README", ignoreCase = true) || name.endsWith(".txt") || name.endsWith(".md"))) {
                        try {
                            readmeContent = zip.bufferedReader(Charsets.UTF_8).readText().take(3000)
                        } catch (e: Exception) {}
                    }
                    count++
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {}

        val sb = StringBuilder()
        sb.appendLine("### Archive: `$fileName` (${formatFileSize(fileSize)})\n")
        if (entries.isNotEmpty()) {
            sb.appendLine("**Files contained (${entries.size}):**")
            entries.take(80).forEach { sb.appendLine(it) }
            if (entries.size > 80) {
                sb.appendLine("... and ${entries.size - 80} more files")
            }
        } else {
            sb.appendLine("*(Archive structure could not be decompressed)*")
        }
        if (!readmeContent.isNullOrBlank()) {
            sb.appendLine("\n**Embedded README:**\n$readmeContent")
        }

        val full = sb.toString().trim()
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = ext,
            fileSizeBytes = fileSize,
            category = AttachmentCategory.DOCUMENT,
            extractedText = full,
            previewSnippet = "Archive with ${entries.size} files"
        )
    }

    private fun processTextFile(
        file: File,
        fileName: String,
        ext: String,
        fileSize: Long
    ): ProcessedAttachment {
        val rawText = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                file.readText(Charsets.ISO_8859_1)
            } catch (e2: Exception) {
                extractReadableStrings(file)
            }
        }

        val language = when (ext) {
            "kt" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "json" -> "json"
            "xml" -> "xml"
            "html", "htm" -> "html"
            "css" -> "css"
            "sql" -> "sql"
            "sh", "bash" -> "bash"
            "md", "markdown" -> "markdown"
            "yaml", "yml" -> "yaml"
            "cpp", "c", "h" -> "cpp"
            "cs" -> "csharp"
            "gradle" -> "groovy"
            "toml" -> "toml"
            else -> ""
        }

        val formatted = if (language.isNotBlank()) {
            "### File: `$fileName`\n```$language\n$rawText\n```"
        } else {
            "### File: `$fileName`\n\n$rawText"
        }

        val truncated = if (formatted.length > 80_000) formatted.take(80_000) + "\n\n... [Truncated]" else formatted
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = ext,
            fileSizeBytes = fileSize,
            category = AttachmentCategory.TEXT,
            extractedText = truncated,
            previewSnippet = rawText.take(250) + "..."
        )
    }

    private fun processGenericFile(
        file: File,
        fileName: String,
        ext: String,
        fileSize: Long
    ): ProcessedAttachment {
        val strings = extractReadableStrings(file)
        val sb = StringBuilder()
        sb.appendLine("### File: `$fileName`")
        sb.appendLine("- **Size**: ${formatFileSize(fileSize)}")
        sb.appendLine("- **Format**: ${ext.uppercase().ifBlank { "Binary" }}")

        if (strings.isNotBlank()) {
            sb.appendLine("\n**Extracted Text / Metadata Strings:**")
            sb.appendLine(strings.take(8000))
        }

        val text = sb.toString().trim()
        return ProcessedAttachment(
            fileName = fileName,
            fileExtension = ext.ifBlank { "bin" },
            fileSizeBytes = fileSize,
            category = AttachmentCategory.DOCUMENT,
            extractedText = text,
            previewSnippet = "$fileName (${formatFileSize(fileSize)})"
        )
    }

    // --- Safe Text Parsing Helpers (Non-throwing, Zero-failure) ---

    private fun extractTextFromOpenXml(xml: String): String {
        val sb = StringBuilder()
        val regex = Regex("<(?:w:|a:)?t[^>]*>(.*?)</(?:w:|a:)?t>|<(?:w:|a:)?(?:p|br|tr)[ />]|<w:tab/>")
        var lastWasNewline = false
        for (match in regex.findAll(xml)) {
            val value = match.value
            when {
                value.contains("<w:tab") -> {
                    sb.append("\t")
                    lastWasNewline = false
                }
                value.startsWith("<w:t") || value.startsWith("<a:t") -> {
                    val text = match.groupValues[1]
                    sb.append(unescapeXml(text))
                    lastWasNewline = false
                }
                else -> {
                    if (!lastWasNewline) {
                        sb.append("\n")
                        lastWasNewline = true
                    }
                }
            }
        }
        val result = sb.toString().trim()
        return if (result.isNotBlank()) result else unescapeXml(xml.replace(Regex("<[^>]+>"), " ")).trim()
    }

    private fun extractTextFromOdfXml(xml: String): String {
        val sb = StringBuilder()
        val regex = Regex("<text:(?:p|h)[^>]*>(.*?)</text:(?:p|h)>")
        for (match in regex.findAll(xml)) {
            val pContent = match.groupValues[1]
            val text = unescapeXml(pContent.replace(Regex("<[^>]+>"), "")).trim()
            if (text.isNotBlank()) {
                sb.appendLine(text)
            }
        }
        return sb.toString().trim()
    }

    private fun convertOdsXmlToMarkdown(xml: String, fileName: String): String {
        val rows = mutableListOf<List<String>>()
        val rowRegex = Regex("(?s)<table:table-row[^>]*>(.*?)</table:table-row>")
        val cellRegex = Regex("(?s)<table:table-cell[^>]*>(.*?)</table:table-cell>")

        for (rowMatch in rowRegex.findAll(xml)) {
            if (rows.size >= 100) break
            val rowContent = rowMatch.groupValues[1]
            val cells = mutableListOf<String>()
            for (cellMatch in cellRegex.findAll(rowContent)) {
                val text = unescapeXml(cellMatch.groupValues[1].replace(Regex("<[^>]+>"), "")).trim()
                cells.add(text.replace("|", "&#124;"))
            }
            if (cells.any { it.isNotBlank() }) {
                rows.add(cells)
            }
        }

        if (rows.isEmpty()) return "*(Empty spreadsheet)*"

        val maxCols = rows.maxOf { it.size }.coerceAtMost(20)
        val sb = StringBuilder()
        sb.appendLine("### Spreadsheet: `$fileName`\n")
        for (i in rows.indices) {
            val r = rows[i].take(maxCols)
            val padded = r + List((maxCols - r.size).coerceAtLeast(0)) { "" }
            sb.append("| ").append(padded.joinToString(" | ")).appendLine(" |")
            if (i == 0) {
                sb.append("| ").append(List(maxCols) { "---" }.joinToString(" | ")).appendLine(" |")
            }
        }
        return sb.toString()
    }

    private fun extractSharedStrings(xml: String): List<String> {
        val list = mutableListOf<String>()
        val siRegex = Regex("(?s)<si>(.*?)</si>")
        val tRegex = Regex("<t[^>]*>(.*?)</t>")
        for (siMatch in siRegex.findAll(xml)) {
            val siContent = siMatch.groupValues[1]
            val tMatches = tRegex.findAll(siContent)
            val combined = tMatches.joinToString("") { unescapeXml(it.groupValues[1]) }
            list.add(combined)
        }
        return list
    }

    private fun convertSheetXmlToMarkdown(xml: String, sharedStrings: List<String>): String {
        val rows = mutableListOf<List<String>>()
        val rowRegex = Regex("(?s)<row[^>]*>(.*?)</row>")
        val cellRegex = Regex("(?s)<c([^>]*)>(.*?)</c>")
        val vRegex = Regex("<v>(.*?)</v>")
        val isRegex = Regex("<t[^>]*>(.*?)</t>")
        val tAttrRegex = Regex("t=\"([a-z])\"")

        val maxRows = 120
        for (rowMatch in rowRegex.findAll(xml)) {
            if (rows.size >= maxRows) break
            val rowContent = rowMatch.groupValues[1]
            val rowCells = mutableListOf<String>()

            for (cellMatch in cellRegex.findAll(rowContent)) {
                val attrs = cellMatch.groupValues[1]
                val inner = cellMatch.groupValues[2]
                val type = tAttrRegex.find(attrs)?.groupValues?.get(1)

                val value = when (type) {
                    "s" -> {
                        val idx = vRegex.find(inner)?.groupValues?.get(1)?.toIntOrNull()
                        if (idx != null && idx in sharedStrings.indices) {
                            sharedStrings[idx]
                        } else ""
                    }
                    "inlineStr" -> {
                        isRegex.find(inner)?.groupValues?.get(1)?.let { unescapeXml(it) } ?: ""
                    }
                    else -> {
                        vRegex.find(inner)?.groupValues?.get(1)?.let { unescapeXml(it) } ?: ""
                    }
                }
                rowCells.add(value.replace("|", "&#124;").replace("\n", " ").trim())
            }
            if (rowCells.any { it.isNotBlank() }) {
                rows.add(rowCells)
            }
        }

        if (rows.isEmpty()) return "*(Empty sheet)*"

        val maxCols = rows.maxOf { it.size }.coerceAtMost(25)
        val sb = StringBuilder()
        for (i in rows.indices) {
            val r = rows[i].take(maxCols)
            val padded = r + List((maxCols - r.size).coerceAtLeast(0)) { "" }
            sb.append("| ").append(padded.joinToString(" | ")).appendLine(" |")
            if (i == 0) {
                sb.append("| ").append(List(maxCols) { "---" }.joinToString(" | ")).appendLine(" |")
            }
        }
        return sb.toString().trim()
    }

    private fun extractReadableStrings(file: File): String {
        return try {
            val bytes = file.readBytes()
            var cur = StringBuilder()

            // 1. Check for UTF-16LE strings (common in Microsoft Office .doc / .ppt)
            var i = 0
            var foundUtf16Count = 0
            val utf16Sb = StringBuilder()
            while (i < bytes.size - 1) {
                val b1 = bytes[i].toInt() and 0xFF
                val b2 = bytes[i + 1].toInt() and 0xFF
                if (b2 == 0 && (b1 in 32..126 || b1 in 160..255 || b1 == 10 || b1 == 13 || b1 == 9)) {
                    cur.append(b1.toChar())
                    i += 2
                } else {
                    if (cur.length >= 4) {
                        val str = cur.toString().trim()
                        if (str.isNotBlank()) {
                            utf16Sb.append(str).append("\n")
                            foundUtf16Count++
                        }
                    }
                    cur.clear()
                    i += 1
                }
            }
            if (cur.length >= 4) {
                val str = cur.toString().trim()
                if (str.isNotBlank()) {
                    utf16Sb.append(str).append("\n")
                }
            }

            // 2. Check for ASCII/UTF-8 strings
            cur.clear()
            val asciiSb = StringBuilder()
            for (b in bytes) {
                val c = b.toInt() and 0xFF
                if (c in 32..126 || c == 10 || c == 13 || c == 9) {
                    cur.append(c.toChar())
                } else {
                    if (cur.length >= 4) {
                        val str = cur.toString().trim()
                        if (str.isNotBlank() && !str.all { it.isDigit() }) {
                            asciiSb.append(str).append("\n")
                        }
                    }
                    cur.clear()
                }
            }
            if (cur.length >= 4) {
                val str = cur.toString().trim()
                if (str.isNotBlank()) {
                    asciiSb.append(str).append("\n")
                }
            }

            val result = if (foundUtf16Count > 10) {
                utf16Sb.toString()
            } else {
                val combined = asciiSb.toString()
                if (combined.length > utf16Sb.length) combined else utf16Sb.toString()
            }

            if (result.length > 50_000) result.take(50_000) + "\n... [Truncated]" else result
        } catch (e: Exception) {
            ""
        }
    }

    private fun unescapeXml(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}
