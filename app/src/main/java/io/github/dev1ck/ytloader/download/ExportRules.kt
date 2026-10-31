package io.github.dev1ck.ytloader.download

import java.text.Normalizer

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

    /**
     * 저장할 이름을 완성형(NFC)으로. Mac에서 입력된 YouTube 제목은 한글이 자모 분리형(NFD)일 수 있고,
     * 그대로 저장하면 Windows로 옮겼을 때 `ㅇㅏㅇㅣㅇㅠ`처럼 풀어져 보인다.
     */
    fun normalizedName(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFC)
}
