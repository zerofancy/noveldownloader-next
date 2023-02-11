package top.ntutn.noveldownloader.downloader

import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import java.io.File
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
        chapters.sortedBy { it.first }.forEach { pair ->
            val content = File(tmpDir, "${pair.first}.html")
            content.inputStream().use {
                book.addSection(pair.second, Resource(it, "${pair.first}.html"))
            }
        }
        val outputFile = File("book.epub")
        outputFile.outputStream().use {
            EpubWriter().write(book, it)
        }
        println("文件写入$outputFile")
    }
}