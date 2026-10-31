package io.github.ytloader.download

import io.github.ytloader.engine.YtOption
import java.util.Locale

/**
 * 제목 미리보기: 영상 제목과(재생목록이면) 재생목록 이름, 그리고 같은 조회로 얻는 부가 정보.
 * 재생목록 평면 조회 항목에는 포맷 정보가 없어 [video]·[audio]가 null일 수 있다.
 */
data class TitleInfo(
    val title: String,
    val playlistTitle: String?,
    val playlistCount: Int? = null,
    val channel: String? = null,
    val durationSec: Long? = null,
    val thumbnailUrl: String? = null,
    val video: VideoInfo? = null,
    val audio: AudioInfo? = null,
)

/** 원본 최고 화질. [hdr]: dynamic_range가 SDR이 아님. */
data class VideoInfo(val height: Int, val fps: Int?, val hdr: Boolean, val codec: String?)

/** 원본 최고 음질. */
data class AudioInfo(val codec: String?, val kbps: Int?, val sampleRate: Int?)

object TitlePreview {
    private const val SEPARATOR = "\t"

    // 제목에 탭이 들어 있을 수 있으므로 제목을 마지막에 둔다.
    private val fields = listOf(
        "playlist_title", "playlist_count", "channel,uploader", "duration", "thumbnail,thumbnails.-1.url",
        "height", "fps", "dynamic_range", "vcodec", "acodec", "abr", "asr", "title",
    )

    /**
     * `--print` 한 줄에 [fields]를 탭으로 이어 받는다. 재생목록 URL이어도 첫 항목만
     * 평면(flat) 조회하므로 빠르다. [playlist]가 false면 `--no-playlist`로 영상 하나만 본다.
     * `-f bv*+ba/b`로 고른 결과의 height·vcodec은 최고 화질 영상, acodec·abr은 최고 음질 오디오다.
     */
    fun options(playlist: Boolean, cookieFile: String?): List<YtOption> = buildList {
        add(YtOption("--simulate"))
        add(YtOption("--flat-playlist"))
        add(YtOption("--playlist-items", "1"))
        add(YtOption(if (playlist) "--yes-playlist" else "--no-playlist"))
        add(YtOption("--no-warnings"))
        if (cookieFile != null) add(YtOption("--cookies", cookieFile))
        add(YtOption("-f", "bv*+ba/b"))
        add(YtOption("--print", fields.joinToString(SEPARATOR) { "%($it|)s" }))
    }

    /** stdout → 정보. 쓸 수 있는 줄이 없으면 null. */
    fun parse(stdout: String): TitleInfo? {
        val line = stdout.lineSequence().map { it.trimEnd('\r') }
            .firstOrNull { it.split(SEPARATOR).size >= fields.size } ?: return null
        val parts = line.split(SEPARATOR, limit = fields.size).map { value ->
            value.trim().takeIf { it.isNotEmpty() && it != "NA" }
        }
        val v = fields.zip(parts).toMap()
        val playlistTitle = v["playlist_title"]
        val title = v["title"] ?: playlistTitle ?: return null
        val height = v["height"]?.toNumber()?.toInt()?.takeIf { it > 0 }
        val acodec = v["acodec"]?.takeIf { it != "none" }
        val abr = v["abr"]?.toNumber()?.takeIf { it > 0 }
        return TitleInfo(
            title = title,
            playlistTitle = playlistTitle,
            playlistCount = v["playlist_count"]?.toNumber()?.toInt(),
            channel = v["channel,uploader"],
            durationSec = v["duration"]?.toNumber()?.toLong(),
            thumbnailUrl = v["thumbnail,thumbnails.-1.url"]?.takeIf { it.startsWith("http") },
            video = height?.let {
                VideoInfo(it, v["fps"]?.toNumber()?.toInt(), v["dynamic_range"].let { r -> r != null && r != "SDR" }, v["vcodec"]?.takeIf { it != "none" })
            },
            audio = if (acodec != null || abr != null) {
                AudioInfo(acodec, abr?.let { Math.round(it).toInt() }, v["asr"]?.toNumber()?.toInt())
            } else null,
        )
    }

    private fun String.toNumber(): Double? = toDoubleOrNull()

    /** `634` → `10:34`, `3735` → `1:02:15`. */
    fun durationLabel(seconds: Long): String {
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** `2160p60 HDR · AV1`. 30fps 이하는 fps를 붙이지 않는다. */
    fun videoLabel(video: VideoInfo): String = buildString {
        append(video.height).append('p')
        if (video.fps != null && video.fps > 30) append(video.fps)
        if (video.hdr) append(" HDR")
        codecName(video.codec)?.let { append(" · ").append(it) }
    }

    /** `Opus 129kbps · 48kHz`. */
    fun audioLabel(audio: AudioInfo): String = listOfNotNull(
        listOfNotNull(codecName(audio.codec), audio.kbps?.let { "${it}kbps" }).joinToString(" ").ifEmpty { null },
        audio.sampleRate?.let { if (it % 1000 == 0) "${it / 1000}kHz" else "%.1fkHz".format(Locale.ROOT, it / 1000.0) },
    ).joinToString(" · ")

    /** yt-dlp 코덱 문자열(`av01.0.13M.08`, `mp4a.40.2` 등) → 사람이 읽는 이름. */
    fun codecName(codec: String?): String? {
        val c = codec?.lowercase() ?: return null
        return when {
            c.startsWith("av01") -> "AV1"
            c.startsWith("vp09") || c.startsWith("vp9") -> "VP9"
            c.startsWith("avc") -> "H.264"
            c.startsWith("hev") || c.startsWith("hvc") -> "HEVC"
            c.startsWith("mp4a") -> "AAC"
            c == "opus" -> "Opus"
            else -> c.substringBefore('.').uppercase()
        }
    }
}
