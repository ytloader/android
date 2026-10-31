package io.github.dev1ck.ytloader.download

import io.github.dev1ck.ytloader.engine.YtOption
import org.junit.Assert.assertEquals
import org.junit.Test

/** ARCHITECTURE 5장 골든 예제 E1–E8. */
class YtDlpArgsBuilderTest {
    private val w = "/data/user/0/app/files/work/job"
    private val c = "/data/user/0/app/no_backup/cookies.txt"
    private val single = "$w/%(title)s.%(ext)s"
    private val playlist = "$w/%(playlist_title|Playlist)s/%(playlist_index|0)03d - %(title)s.%(ext)s"
    private val tail = listOf("--windows-filenames", "--newline", "--progress", "-o")

    private fun tokens(options: DownloadOptions, cookie: String? = null): List<String> =
        YtDlpArgsBuilder.build(options, w, cookie).flatMap { listOfNotNull(it.name, it.value) }

    private fun audio(format: String, quality: String, thumb: Boolean, meta: Boolean, playlist: Boolean = false) =
        DownloadOptions(Mode.AUDIO, format, quality, thumb, meta, playlist)

    private fun video(format: String, res: String, thumb: Boolean, meta: Boolean, playlist: Boolean = false) =
        DownloadOptions(Mode.VIDEO, format, res, thumb, meta, playlist)

    @Test fun e1_mp3() = assertEquals(
        listOf("-f", "bestaudio/best", "-x", "--audio-format", "mp3", "--audio-quality", "0",
            "--embed-thumbnail", "--embed-metadata", "--no-playlist") + tail + single,
        tokens(audio("mp3", "0", thumb = true, meta = true)),
    )

    @Test fun e2_flac_ignoresQuality() = assertEquals(
        listOf("-f", "bestaudio/best", "-x", "--audio-format", "flac",
            "--embed-thumbnail", "--no-playlist") + tail + single,
        tokens(audio("flac", "320K", thumb = true, meta = false)),
    )

    @Test fun e3_audioOriginal_noThumbnail() = assertEquals(
        listOf("-f", "bestaudio/best", "--embed-metadata", "--no-playlist") + tail + single,
        tokens(audio("original", "0", thumb = true, meta = true)),
    )

    @Test fun e4_m4a_withCookies() = assertEquals(
        listOf("-f", "bestaudio/best", "-x", "--audio-format", "m4a", "--audio-quality", "192K",
            "--embed-metadata", "--cookies", c, "--no-playlist") + tail + single,
        tokens(audio("m4a", "192K", thumb = false, meta = true), c),
    )

    @Test fun e5_mp4_1080() = assertEquals(
        listOf("-f", "bv*+ba/b", "-S", "res:1080,ext:mp4:m4a", "--merge-output-format", "mp4",
            "--remux-video", "mp4", "--embed-thumbnail", "--embed-metadata", "--no-playlist") + tail + single,
        tokens(video("mp4", "1080", thumb = true, meta = true)),
    )

    @Test fun e6_webm_best_noThumbnail() = assertEquals(
        listOf("-f", "bv*+ba/b", "-S", "ext:webm:webm", "--merge-output-format", "webm",
            "--remux-video", "webm", "--embed-metadata", "--no-playlist") + tail + single,
        tokens(video("webm", "", thumb = true, meta = true)),
    )

    @Test fun e7_videoOriginal_playlist_cookies() = assertEquals(
        listOf("-f", "bv*+ba/b", "--embed-metadata", "--cookies", c, "--yes-playlist") + tail + playlist,
        tokens(video("original", "", thumb = true, meta = true, playlist = true), c),
    )

    @Test fun e8_mkv_720() = assertEquals(
        listOf("-f", "bv*+ba/b", "-S", "res:720", "--merge-output-format", "mkv", "--remux-video", "mkv",
            "--no-playlist") + tail + single,
        tokens(video("mkv", "720", thumb = false, meta = false)),
    )

    @Test fun outputTemplateIsSingleToken() {
        val options = YtDlpArgsBuilder.build(video("original", "", thumb = false, meta = false, playlist = true), w, null)
        assertEquals(YtOption("-o", playlist), options.last())
    }
}
