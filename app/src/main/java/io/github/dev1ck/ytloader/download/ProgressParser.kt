package io.github.dev1ck.ytloader.download

sealed interface ParsedLine {
    data class Progress(val percent: Float, val text: String) : ParsedLine
    data class PlaylistItem(val index: Int, val count: Int, val text: String) : ParsedLine
    data class PostProcessing(val tag: String, val text: String) : ParsedLine
    data class Error(val line: String) : ParsedLine
}

/** yt-dlp 출력 한 줄 → 상태. 데스크톱판 Update-ProgressFromLine과 같은 규칙. */
object ProgressParser {
    private val progress = Regex("""^\[download\]\s+([\d.]+)%(.*)$""")
    private val playlistItem = Regex("""^\[download\] Downloading item (\d+) of (\d+)""")
    private val postProcessing =
        Regex("""^\[(ExtractAudio|Merger|VideoRemuxer|VideoConvertor|EmbedThumbnail|Metadata|FixupM4a|FixupM3u8)\]""")
    private val whitespace = Regex("""\s+""")

    /** 매칭 없으면 null (상태 유지). */
    fun parse(line: String): ParsedLine? {
        progress.find(line)?.let { m ->
            val (percent, rest) = m.destructured
            val value = percent.toFloatOrNull() ?: return null
            return ParsedLine.Progress(value, "다운로드 중... $percent%${rest.replace(whitespace, " ")}")
        }
        playlistItem.find(line)?.let { m ->
            val (index, count) = m.destructured
            return ParsedLine.PlaylistItem(index.toInt(), count.toInt(), "재생목록 $index / $count 항목 처리 중...")
        }
        postProcessing.find(line)?.let { m ->
            val tag = m.groupValues[1]
            return ParsedLine.PostProcessing(tag, "변환 / 후처리 중... [$tag]")
        }
        if (line.startsWith("ERROR:")) return ParsedLine.Error(line)
        return null
    }
}
