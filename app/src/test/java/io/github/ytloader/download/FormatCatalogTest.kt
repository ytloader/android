package io.github.ytloader.download

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC 6장 표와 값·라벨·순서·플래그 일치. */
class FormatCatalogTest {
    @Test fun audioFormats() = assertEquals(
        listOf(
            FormatOption("mp3", "MP3 (범용)", true, true),
            FormatOption("m4a", "M4A (AAC)", true, true),
            FormatOption("opus", "OPUS (고효율)", true, true),
            FormatOption("flac", "FLAC (무손실)", false, true),
            FormatOption("wav", "WAV (무손실)", false, false),
            FormatOption("original", "원본 (변환 없음)", false, false),
        ),
        FormatCatalog.audioFormats,
    )

    @Test fun audioQualities() = assertEquals(
        listOf("0" to "최고 (VBR)", "320K" to "320 kbps", "256K" to "256 kbps", "192K" to "192 kbps", "128K" to "128 kbps"),
        FormatCatalog.audioQualities.map { it.value to it.label },
    )

    @Test fun videoFormats() = assertEquals(
        listOf(
            Triple("mp4", "MP4 (호환성 높음)", true),
            Triple("mkv", "MKV", true),
            Triple("webm", "WEBM", false),
            Triple("original", "원본 (변환 없음)", false),
        ),
        FormatCatalog.videoFormats.map { Triple(it.value, it.label, it.supportsThumbnail) },
    )

    @Test fun videoFormatsAlwaysHaveResolution() =
        assertEquals(true, FormatCatalog.videoFormats.all { it.hasQuality })

    @Test fun videoResolutions() = assertEquals(
        listOf("" to "최고 화질", "2160" to "2160p (4K)", "1440" to "1440p", "1080" to "1080p",
            "720" to "720p", "480" to "480p", "360" to "360p"),
        FormatCatalog.videoResolutions.map { it.value to it.label },
    )

    @Test fun unknownValueFallsBackToFirst() {
        assertEquals("mp3", FormatCatalog.format(Mode.AUDIO, "mp4").value)
        assertEquals("mp4", FormatCatalog.format(Mode.VIDEO, null).value)
        assertEquals("0", FormatCatalog.quality(Mode.AUDIO, "999K").value)
        assertEquals("", FormatCatalog.quality(Mode.VIDEO, "0").value)
        assertEquals("1080", FormatCatalog.quality(Mode.VIDEO, "1080").value)
    }
}
