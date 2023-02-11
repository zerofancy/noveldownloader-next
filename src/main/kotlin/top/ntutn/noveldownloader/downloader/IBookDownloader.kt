package top.ntutn.noveldownloader.downloader

/**
 * 针对某个特定网站的下载处理器
 */
interface IBookDownloader {
    /**
     * 判断当前处理器是否匹配
     * @param url 小说详情页地址
     */
    fun matchBookInfo(url: String): Boolean

    fun getBookInfo(url: String): BookInfo

    fun downloadChapters(saver: IChapterSaver, onComplete: () -> Unit)
}
