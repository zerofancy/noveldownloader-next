package top.ntutn.noveldownloader.downloader

/**
 * 可用于保存章节信息
 */
interface IChapterSaver {
    fun saveChapter(num: Int, title: String, content: String)

    fun saveBook()
}
