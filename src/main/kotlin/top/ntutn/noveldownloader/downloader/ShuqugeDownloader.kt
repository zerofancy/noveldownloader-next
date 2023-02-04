package top.ntutn.noveldownloader.downloader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.util.regex.Pattern

class ShuqugeDownloader: IBookDownloader {
    private val urlRegex = Pattern.compile("http.*?www\\.ishuquge\\.org/txt/(.*?)/index\\.html")
    private val authorRegex = Pattern.compile("作者：(.*?)\\s")

    private var indexUrl = ""
    private var indexDocument: Document? = null
    override fun matchBookInfo(url: String): Boolean {
        return urlRegex.matcher(url).matches()
    }

    override fun getBookInfo(url: String): BookInfo {
        val document = Jsoup.parse(URL(url), 5_000)
        val title = document.selectFirst(".info > h2:nth-child(2)")?.text() ?: ""
        val cover = document.selectFirst(".cover > img:nth-child(1)")?.attr("src") ?: ""
        val smallDivContent = document.selectFirst(".small")?.text() ?: ""
        val authorMatcher = authorRegex.matcher(smallDivContent)
        val author = if (authorMatcher.find()) authorMatcher.group(1) else ""

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

        val chart = document.selectFirst(".listmain > dl:nth-child(1)")

        var dts = 0
        val links = mutableListOf<Triple<Int, String, String>>()
        chart?.children()?.forEachIndexed { index, element ->
            if (element.`is`("dt")) {
                dts++
                return@forEachIndexed
            }
            if (dts < 2) {
                return@forEachIndexed
            }
            val link = element.child(0)
            val title = link.text()
            val href = link.attr("href")

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
        contentEle.select("#center_tip").remove()
        val content = contentEle.html()
        saver.saveChapter(num, title, content)
    }

    private fun retryOnFailure(times: Int, block: () -> Unit) {
        repeat(times) { tried ->
            kotlin.runCatching {
                block()
            }.onSuccess {
                return
            }.onFailure {
                println("failed ${tried + 1}")
                it.printStackTrace()
            }
        }
    }
}