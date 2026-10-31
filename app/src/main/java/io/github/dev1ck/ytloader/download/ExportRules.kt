package io.github.dev1ck.ytloader.download

/** ARCHITECTURE 8장: 작업 폴더에서 공용 저장소로 내보낼 최종 파일인지 (yt-dlp 중간 산출물 제외). */
object ExportRules {
    private val tempExtensions = setOf("part", "ytdl", "temp", "frag")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val formatStream = Regex("""\.f\d+\.[A-Za-z0-9]+$""")

    fun isExportable(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            ext in tempExtensions -> false
            name.contains(".part-Frag") -> false
            name.contains(".temp.") -> false
            formatStream.containsMatchIn(name) -> false
            ext in imageExtensions -> false
            else -> true
        }
    }
}
