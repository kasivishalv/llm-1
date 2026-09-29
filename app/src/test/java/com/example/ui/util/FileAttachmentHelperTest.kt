package com.example.ui.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class FileAttachmentHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testProcessPlainTextFile() = runBlocking {
        val testFile = File(context.cacheDir, "sample.txt")
        testFile.writeText("Hello world! This is a test plain text document.")

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals("sample.txt", result.fileName)
        assertEquals("txt", result.fileExtension)
        assertEquals(AttachmentCategory.TEXT, result.category)
        assertTrue(result.extractedText!!.contains("Hello world!"))
    }

    @Test
    fun testProcessCsvFile() = runBlocking {
        val testFile = File(context.cacheDir, "data.csv")
        testFile.writeText("Name,Age,Role\nAlice,30,Engineer\nBob,25,Designer\n")

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals("csv", result.fileExtension)
        assertEquals(AttachmentCategory.SPREADSHEET, result.category)
        assertTrue(result.extractedText!!.contains("| Alice | 30 | Engineer |"))
    }

    @Test
    fun testProcessEmptyFileGracefully() = runBlocking {
        val testFile = File(context.cacheDir, "empty.doc")
        testFile.writeBytes(ByteArray(0))

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals(0L, result.fileSizeBytes)
        assertTrue(result.extractedText!!.contains("empty"))
    }

    @Test
    fun testProcessDocxFile() = runBlocking {
        val testFile = File(context.cacheDir, "test.docx")
        ZipOutputStream(FileOutputStream(testFile)).use { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                    <w:body>
                        <w:p><w:r><w:t>First Paragraph in Word</w:t></w:r></w:p>
                        <w:p><w:r><w:t>Second line of text</w:t></w:r></w:p>
                    </w:body>
                </w:document>
            """.trimIndent()
            zos.write(xml.toByteArray())
            zos.closeEntry()
        }

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals(AttachmentCategory.DOCUMENT, result.category)
        assertTrue(result.extractedText!!.contains("First Paragraph in Word"))
        assertTrue(result.extractedText!!.contains("Second line of text"))
    }

    @Test
    fun testProcessXlsxFile() = runBlocking {
        val testFile = File(context.cacheDir, "test.xlsx")
        ZipOutputStream(FileOutputStream(testFile)).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            val sharedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <si><t>Revenue</t></si>
                    <si><t>Expenses</t></si>
                </sst>
            """.trimIndent()
            zos.write(sharedXml.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <sheetData>
                        <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1"><v>50000</v></c></row>
                        <row r="2"><c r="A2" t="s"><v>1</v></c><c r="B2"><v>30000</v></c></row>
                    </sheetData>
                </worksheet>
            """.trimIndent()
            zos.write(sheetXml.toByteArray())
            zos.closeEntry()
        }

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals(AttachmentCategory.SPREADSHEET, result.category)
        assertTrue(result.extractedText!!.contains("Revenue"))
        assertTrue(result.extractedText!!.contains("50000"))
    }

    @Test
    fun testProcessPptxFile() = runBlocking {
        val testFile = File(context.cacheDir, "test.pptx")
        ZipOutputStream(FileOutputStream(testFile)).use { zos ->
            zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
            val slideXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                    <p:cSld>
                        <p:spTree>
                            <p:sp>
                                <p:txBody>
                                    <a:p><a:r><a:t>Quarterly Business Review</a:t></a:r></a:p>
                                    <a:p><a:r><a:t>Key Achievements in 2026</a:t></a:r></a:p>
                                </p:txBody>
                            </p:sp>
                        </p:spTree>
                    </p:cSld>
                </p:sld>
            """.trimIndent()
            zos.write(slideXml.toByteArray())
            zos.closeEntry()
        }

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals(AttachmentCategory.PRESENTATION, result.category)
        assertTrue(result.extractedText!!.contains("Quarterly Business Review"))
    }

    @Test
    fun testProcessUnknownBinaryFileNeverFails() = runBlocking {
        val testFile = File(context.cacheDir, "custom.unknown_bin")
        testFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x41, 0x42, 0x43, 0x44, 0x00))

        val uri = Uri.fromFile(testFile)
        val result = FileAttachmentHelper.processAttachment(context, uri)

        assertNotNull(result)
        assertEquals("unknown_bin", result.fileExtension)
        assertNotNull(result.extractedText)
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileAttachmentHelper.formatFileSize(0))
        assertEquals("500 B", FileAttachmentHelper.formatFileSize(500))
        assertEquals("1.5 KB", FileAttachmentHelper.formatFileSize(1536))
        assertEquals("2.0 MB", FileAttachmentHelper.formatFileSize(2 * 1024 * 1024))
    }
}
