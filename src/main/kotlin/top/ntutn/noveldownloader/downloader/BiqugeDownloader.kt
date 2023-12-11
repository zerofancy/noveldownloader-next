package top.ntutn.noveldownloader.downloader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.util.regex.Pattern

/**
 * [笔趣阁](https://www.biduoxs.cc/)
 */
class BiqugeDownloader: IBookDownloader {
    private val urlRegex = Pattern.compile("https://www.biduoxs.cc/biquge/(.*?)/")
    private val authorRegex = Pattern.compile("作\\S*?者：(.*?)\\s")

    private var indexUrl = ""
    private var indexDocument: Document? = null
    override fun matchBookInfo(url: String): Boolean {
        return urlRegex.matcher(url).matches()
    }

    override fun getBookInfo(url: String): BookInfo {
        val document = retryOnFailure(5) { Jsoup.parse(URL(url), 5_000) }
        val title = document.selectFirst("h1")?.text() ?: ""
        val cover = document.selectFirst("#fmimg > img")?.attr("src") ?: ""
        val smallDivContent = document.selectFirst("#info")?.text() ?: ""
        val authorMatcher = authorRegex.matcher(smallDivContent)
        val author = if (authorMatcher.find()) authorMatcher.group(1).trim() else ""

        indexUrl = url
        indexDocument = document

        return BookInfo(title, author, cover)
    }

    override fun downloadChapters(saver: IChapterSaver, onComplete: () -> Unit) {
        val document = indexDocument
        if (document == null) {
            println("文档解析异常")
            onComplete()
            return
        }

        val chart = document.selectFirst("#list")

        val links = mutableListOf<Triple<Int, String, String>>()
        chart?.select("a")?.forEachIndexed { index, element ->
            val title = element.text()
            val href = element.attr("href")

            println("$index 发现正文，title=$title, href=$href")
            links.add(Triple(index, title, href))
        }

        println("开始下载进程")

        links.forEach {
            downloadArticle(it.first, it.third, saver)
        }
        onComplete()
    }

    private fun downloadArticle(num: Int, link: String, saver: IChapterSaver) = retryOnFailure(5) {
        val document = Jsoup.parse(URL(URL(indexUrl), link), 5_000)

        val title = document.selectFirst("h1")?.text() ?: ""
        val contentEle = document.selectFirst("#content") ?: throw RuntimeException("未找到正文")
        contentEle.select("div").remove()
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