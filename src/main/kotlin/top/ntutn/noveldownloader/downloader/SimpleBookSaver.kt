package top.ntutn.noveldownloader.downloader

import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException
import kotlin.random.Random

class SimpleBookSaver(private val bookInfo: BookInfo): IChapterSaver {
    private var chapters= mutableListOf<Pair<Int, String>>()
    private val tmpDir = File("/tmp/SimpleBookSaver/" + Random.nextLong())

    init {
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
        }
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()
    }

    override fun saveChapter(num: Int, title: String, content: String) {
        val file = File(tmpDir, "$num.html")
        val htmlContent = """
            <html lang="zh-Hans">
            <head>
                <meta charset="UTF-8" />
                <title>$title</title>
            </head>
            <body>
            <h1>$title</h1>
            $content
            </body>
            </html>
        """.trimIndent()

        file.writeText(htmlContent)
        chapters.add(num to title)

        println("保存章节$num $title")
    }

    override fun saveBook() {
        val book = Book()
        book.metadata.apply {
            addTitle(bookInfo.title)
            addAuthor(Author(bookInfo.author))
        }
        try {
            val localCoverFile = File(tmpDir, "cover.png")
            downloadUsingNIO(bookInfo.coverImage!!, localCoverFile)
            localCoverFile.inputStream().use {
                book.coverImage = Resource(it, "book_cover.png")
            }
            println("设定封面 ${bookInfo.coverImage}")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        chapters.sortedBy { it.first }.forEach { pair ->
            val content = File(tmpDir, "${pair.first}.html")
            content.inputStream().use {
                book.addSection(pair.second, Resource(it, "${pair.first}.html"))
            }
        }
        val outputFile = File("${bookInfo.title.trim()}.epub")
        outputFile.outputStream().use {
            EpubWriter().write(book, it)
        }
        println("文件写入$outputFile")
    }


    @Throws(IOException::class)
    private fun downloadUsingNIO(urlStr: String, file: File) {
        val tmpFile = File.createTempFile("ndl", ".tmp")
        tmpFile.deleteOnExit()
        val url = URL(urlStr)
        val rbc: ReadableByteChannel = Channels.newChannel(url.openStream())
        val fos = FileOutputStream(tmpFile)
        fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
        fos.close()
        rbc.close()

        println("trying un gzip")
        try {
            val gzIns = GZIPInputStream(tmpFile.inputStream())
            val gzOS = FileOutputStream(file)
            gzIns.transferTo(gzOS)
        } catch (e: ZipException) {
            e.printStackTrace()
            tmpFile.copyTo(file, true)
        }
    }
}