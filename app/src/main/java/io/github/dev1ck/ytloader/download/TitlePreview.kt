package io.github.dev1ck.ytloader.download

import io.github.dev1ck.ytloader.engine.YtOption

/** 제목 미리보기: 영상 제목과(재생목록이면) 재생목록 이름. */
data class TitleInfo(val title: String, val playlistTitle: String?)

object TitlePreview {
    private const val SEPARATOR = "\t"

    /**
     * `--print` 한 줄에 `재생목록 이름<TAB>제목`을 받는다. 재생목록 URL이어도 첫 항목만
     * 평면(flat) 조회하므로 빠르다. [playlist]가 false면 `--no-playlist`로 영상 하나만 본다.
     */
    fun options(playlist: Boolean, cookieFile: String?): List<YtOption> = buildList {
        add(YtOption("--simulate"))
        add(YtOption("--flat-playlist"))
        add(YtOption("--playlist-items", "1"))
        add(YtOption(if (playlist) "--yes-playlist" else "--no-playlist"))
        add(YtOption("--no-warnings"))
        if (cookieFile != null) add(YtOption("--cookies", cookieFile))
        add(YtOption("--print", "%(playlist_title|)s$SEPARATOR%(title)s"))
    }

    /** stdout → 정보. 쓸 수 있는 줄이 없으면 null. */
    fun parse(stdout: String): TitleInfo? {
        val line = stdout.lineSequence().map { it.trimEnd('\r') }.firstOrNull { it.contains(SEPARATOR) } ?: return null
        val playlistTitle = line.substringBefore(SEPARATOR).trim().takeIf { it.isNotEmpty() && it != "NA" }
        val title = line.substringAfter(SEPARATOR).trim().takeIf { it.isNotEmpty() && it != "NA" }
        return when {
            title != null -> TitleInfo(title, playlistTitle)
            playlistTitle != null -> TitleInfo(playlistTitle, playlistTitle)
            else -> null
        }
    }
}
