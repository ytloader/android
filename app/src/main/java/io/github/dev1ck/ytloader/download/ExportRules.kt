package io.github.dev1ck.ytloader.download

/** ARCHITECTURE 8장: 작업 폴더에서 공용 저장소로 내보낼 최종 파일인지 (yt-dlp 중간 산출물 제외). */
object ExportRules {
    private val tempExtensions = setOf("part", "ytdl", "temp", "frag")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val formatStream = Regex("""\.f\d+\.[A-Za-z0-9]+$""")

    /**
     * [interruptedItem]: 실패·취소된 재생목록에서 처리 중이던 항목 번호. 그 항목의 파일은
     * 변환 전 원본이나 쓰다 만 결과물일 수 있어 이름만으로는 구별할 수 없으므로 전부 제외한다.
     */
    fun isExportable(name: String, interruptedItem: Int? = null): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            ext in tempExtensions -> false
            name.contains(".part-Frag") -> false
            name.contains(".temp.") -> false
            formatStream.containsMatchIn(name) -> false
            ext in imageExtensions -> false
            interruptedItem != null && name.startsWith("%03d - ".format(interruptedItem)) -> false
            else -> true
        }
    }
}
