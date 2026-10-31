package io.github.dev1ck.ytloader.download

import io.github.dev1ck.ytloader.engine.YtOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitlePreviewTest {
    // yt-dlp 2026.08.19 실제 출력 (2026-10-31).
    private val bunny = "\t\tBlender\t635\thttps://i.ytimg.com/vi_webp/aqz-KE-bpKQ/maxresdefault.webp\t2160\t60\tSDR\t" +
        "av01.0.13M.08\topus\t128.612\t48000\tBig Buck Bunny 60fps 4K - Official Blender Foundation Short Film\n"
    private val zoo = "\t\tjawed\t19\thttps://i.ytimg.com/vi/jNQXAC9IVRw/hqdefault.jpg\t240\t15\tSDR\t" +
        "av01.0.00M.08.0.110.05.01.06.0\topus\t106.064\t48000\tMe at the zoo\r\n"
    private val playlist = "Team Fortress 2 [2010 Version]\t29\tWickman Wish\t629\thttps://i.ytimg.com/vi/RlJy2Kt8Be0/hqdefault.jpg" +
        "\t\t\t\t\t\t\t\tLet's Play Team Fortress 2 [2010] - Intro\n"

    @Test fun singleVideo() = assertEquals(
        TitleInfo(
            title = "Big Buck Bunny 60fps 4K - Official Blender Foundation Short Film",
            playlistTitle = null,
            playlistCount = null,
            channel = "Blender",
            durationSec = 635,
            thumbnailUrl = "https://i.ytimg.com/vi_webp/aqz-KE-bpKQ/maxresdefault.webp",
            video = VideoInfo(2160, 60, hdr = false, codec = "av01.0.13M.08"),
            audio = AudioInfo("opus", 129, 48000),
        ),
        TitlePreview.parse(bunny),
    )

    @Test fun lowResVideo() {
        val info = TitlePreview.parse(zoo)!!
        assertEquals("Me at the zoo", info.title)
        assertEquals("240p · AV1", TitlePreview.videoLabel(info.video!!))
        assertEquals("Opus 106kbps · 48kHz", TitlePreview.audioLabel(info.audio!!))
    }

    @Test fun playlistEntryHasNoFormats() = assertEquals(
        TitleInfo(
            title = "Let's Play Team Fortress 2 [2010] - Intro",
            playlistTitle = "Team Fortress 2 [2010 Version]",
            playlistCount = 29,
            channel = "Wickman Wish",
            durationSec = 629,
            thumbnailUrl = "https://i.ytimg.com/vi/RlJy2Kt8Be0/hqdefault.jpg",
        ),
        TitlePreview.parse(playlist),
    )

    @Test fun titleWithTabKeepsRest() = assertEquals("a\tb", TitlePreview.parse("\t".repeat(12) + "a\tb")?.title)

    @Test fun naValuesIgnored() {
        val info = TitlePreview.parse("NA\tNA\tNA\tNA\tNA\tNA\tNA\tNA\tnone\tnone\tNA\tNA\tx")!!
        assertEquals(TitleInfo("x", null), info)
    }

    @Test fun noUsableLine() {
        assertNull(TitlePreview.parse(""))
        assertNull(TitlePreview.parse("WARNING: something\n"))
        assertNull(TitlePreview.parse("\tMe at the zoo"))
        assertNull(TitlePreview.parse("\t".repeat(12)))
    }

    @Test fun labels() {
        assertEquals("0:19", TitlePreview.durationLabel(19))
        assertEquals("10:35", TitlePreview.durationLabel(635))
        assertEquals("1:02:15", TitlePreview.durationLabel(3735))
        assertEquals("2160p60 HDR · VP9", TitlePreview.videoLabel(VideoInfo(2160, 60, hdr = true, codec = "vp09.02.51.10")))
        assertEquals("1080p · H.264", TitlePreview.videoLabel(VideoInfo(1080, 30, hdr = false, codec = "avc1.640028")))
        assertEquals("AAC 130kbps · 44.1kHz", TitlePreview.audioLabel(AudioInfo("mp4a.40.2", 130, 44100)))
        assertEquals("Opus", TitlePreview.audioLabel(AudioInfo("opus", null, null)))
    }

    @Test fun optionsWithCookiesAndPlaylist() {
        val options = TitlePreview.options(playlist = true, cookieFile = "/c.txt")
        assertEquals(
            listOf(
                YtOption("--simulate"), YtOption("--flat-playlist"), YtOption("--playlist-items", "1"),
                YtOption("--yes-playlist"), YtOption("--no-warnings"), YtOption("--cookies", "/c.txt"),
                YtOption("-f", "bv*+ba/b"),
            ),
            options.dropLast(1),
        )
        assertEquals(
            "%(playlist_title|)s\t%(playlist_count|)s\t%(channel,uploader|)s\t%(duration|)s\t" +
                "%(thumbnail,thumbnails.-1.url|)s\t%(height|)s\t%(fps|)s\t%(dynamic_range|)s\t%(vcodec|)s\t" +
                "%(acodec|)s\t%(abr|)s\t%(asr|)s\t%(title|)s",
            options.last().value,
        )
    }

    @Test fun optionsSingle() =
        assertEquals(YtOption("--no-playlist"), TitlePreview.options(playlist = false, cookieFile = null)[3])
}
