import top.ntutn.noveldownloader.downloader.ShuqugeDownloader
import top.ntutn.noveldownloader.downloader.SimpleBookSaver
import java.util.*

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    println("Novel Downloader")
    print("请输入小说详情页url: ")

    var url = ""
    while (url.isBlank()) {
        url = scanner.nextLine()
    }

    println("你选择了 $url")

    val downloaders = listOf(ShuqugeDownloader())

    val matchedDownloader = downloaders.find { it.matchBookInfo(url) }
    if (matchedDownloader == null) {
        println("未为 $url 找到合适下载器")
        return
    }

    val bookInfo = matchedDownloader.getBookInfo(url)

    println(bookInfo)

    val saver = SimpleBookSaver(bookInfo)
    matchedDownloader.downloadChapters(saver) {
        saver.saveBook()
    }
}