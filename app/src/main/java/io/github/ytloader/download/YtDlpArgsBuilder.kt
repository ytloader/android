package io.github.ytloader.download

import io.github.ytloader.engine.YtOption

/** ARCHITECTURE 5장 규칙. 라이브러리가 붙이는 --ffmpeg-location, --js-runtimes는 넣지 않는다. */
object YtDlpArgsBuilder {
    private val lossyAudioFormats = setOf("mp3", "m4a", "opus")

    /** [cookieFile]이 null이면 쿠키를 쓰지 않는다 (로그인 여부·스위치 판단은 호출 측). */
    fun build(options: DownloadOptions, workDir: String, cookieFile: String?): List<YtOption> {
        val format = FormatCatalog.format(options.mode, options.format)
        val args = mutableListOf<YtOption>()

        if (options.mode == Mode.AUDIO) {
            args += YtOption("-f", "bestaudio/best")
            if (format.value != "original") {
                args += YtOption("-x")
                args += YtOption("--audio-format", format.value)
                if (format.value in lossyAudioFormats) {
                    args += YtOption("--audio-quality", options.quality)
                }
            }
        } else {
            val sort = buildList {
                if (options.quality.isNotEmpty()) add("res:${options.quality}")
                if (format.value == "mp4") add("ext:mp4:m4a")
                if (format.value == "webm") add("ext:webm:webm")
            }
            args += YtOption("-f", "bv*+ba/b")
            if (sort.isNotEmpty()) args += YtOption("-S", sort.joinToString(","))
            if (format.value != "original") {
                args += YtOption("--merge-output-format", format.value)
                args += YtOption("--remux-video", format.value)
            }
        }

        if (format.supportsThumbnail && options.embedThumbnail) args += YtOption("--embed-thumbnail")
        if (options.embedMetadata) args += YtOption("--embed-metadata")
        if (cookieFile != null) args += YtOption("--cookies", cookieFile)
        args += YtOption(if (options.playlist) "--yes-playlist" else "--no-playlist")
        args += YtOption("--windows-filenames")
        args += YtOption("--newline")
        args += YtOption("--progress")
        val template = if (options.playlist) {
            "%(playlist_title|Playlist)s/%(playlist_index|0)03d - %(title)s.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }
        args += YtOption("-o", "$workDir/$template")
        return args
    }
}
