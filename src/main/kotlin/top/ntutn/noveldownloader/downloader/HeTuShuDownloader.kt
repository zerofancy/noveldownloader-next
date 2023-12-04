package top.ntutn.noveldownloader.downloader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.util.regex.Pattern

/**
 * [和图书](https://www.hetushu.com/)
 */
class HeTuShuDownloader: IBookDownloader {
    private val urlRegex = Pattern.compile("https.*?www\\.hetushu\\.com/book/(.*?)/index.html")
    private val authorRegex = Pattern.compile("作者：(.*?)\\s")

    private var indexUrl = ""
    private var indexDocument: Document? = null
    override fun matchBookInfo(url: String): Boolean {
        return urlRegex.matcher(url).matches()
    }

    override fun getBookInfo(url: String): BookInfo {
        val document = retryOnFailure(5) { Jsoup.parse(URL(url), 5_000) }
        val title = document.selectFirst(".book_info > h2")?.text() ?: ""
        val cover = document.selectFirst(".book_info > img")?.attr("src") ?: ""
        val author = document.selectFirst(".book_info > div > a")?.text() ?: ""

        indexUrl = url
        indexDocument = document

        return BookInfo(title, author, URL(URL(indexUrl), cover).toString())
    }

    override fun downloadChapters(saver: IChapterSaver, onComplete: () -> Unit) {
        val document = indexDocument
        if (document == null) {
            println("文档解析异常")
            onComplete()
            return
        }

        val links = document.select("#dir > dd > a").mapIndexed { index, element -> Triple(index, element.text(), element.attr("href")) }

        println("开始下载进程")

        links.forEach {
            println("$it 开始下载${it.second}")
            downloadArticle(it.first, it.third, saver)
        }
        onComplete()
    }

    private fun downloadArticle(num: Int, link: String, saver: IChapterSaver) = retryOnFailure(5) {
        val document = Jsoup.parse(URL(URL(indexUrl), link), 5_000)

        val title = document.selectFirst(".h2")?.text() ?: ""
        val contentEle = document.selectFirst("#content") ?: throw RuntimeException("未找到正文")
        val content = contentEle.html()
        saver.saveChapter(num, title, content)
    }

    private fun <T> retryOnFailure(times: Int, block: () -> T): T {
        var lastErr: Throwable? = null
        repeat(times) { tried ->
            kotlin.runCatching {
                return block()
            }.onFailure {
                println("failed ${tried + 1}")
                lastErr = it
            }
        }
        throw lastErr!!
    }
}