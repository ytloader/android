package io.github.dev1ck.ytloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC FR-5 표. */
class ProgressParserTest {
    @Test fun downloadProgress() = assertEquals(
        ParsedLine.Progress(42.3f, "다운로드 중... 42.3% of 3.2MiB at 1.1MiB/s ETA 00:02"),
        ProgressParser.parse("[download]  42.3% of 3.2MiB at 1.1MiB/s ETA 00:02"),
    )

    @Test fun downloadProgressCollapsesSpaces() = assertEquals(
        ParsedLine.Progress(100f, "다운로드 중... 100.0% of 246.27KiB at 9.51MiB/s ETA 00:00"),
        ProgressParser.parse("[download] 100.0% of  246.27KiB at    9.51MiB/s ETA 00:00"),
    )

    @Test fun playlistItem() = assertEquals(
        ParsedLine.Status("재생목록 3 / 12 항목 처리 중..."),
        ProgressParser.parse("[download] Downloading item 3 of 12"),
    )

    @Test fun postProcessingTags() {
        for (tag in listOf("ExtractAudio", "Merger", "VideoRemuxer", "VideoConvertor", "EmbedThumbnail",
            "Metadata", "FixupM4a", "FixupM3u8")) {
            assertEquals(
                ParsedLine.PostProcessing(tag, "변환 / 후처리 중... [$tag]"),
                ProgressParser.parse("[$tag] Destination: /x/a.mp3"),
            )
        }
    }

    @Test fun error() = assertEquals(
        ParsedLine.Error("ERROR: [youtube] xxxxxxxxxxx: This video is unavailable"),
        ProgressParser.parse("ERROR: [youtube] xxxxxxxxxxx: This video is unavailable"),
    )

    @Test fun unmatchedLines() {
        assertNull(ProgressParser.parse("[youtube] jNQXAC9IVRw: Downloading webpage"))
        assertNull(ProgressParser.parse("[download] Destination: /x/a.webm"))
        assertNull(ProgressParser.parse("[ThumbnailsConvertor] Converting thumbnail"))
        assertNull(ProgressParser.parse("WARNING: something"))
        assertNull(ProgressParser.parse(""))
    }
}
