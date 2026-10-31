package io.github.dev1ck.ytloader.download

import io.github.dev1ck.ytloader.engine.YtOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitlePreviewTest {
    @Test fun singleVideo() = assertEquals(TitleInfo("Me at the zoo", null), TitlePreview.parse("\tMe at the zoo\n"))

    @Test fun naPlaylistTitleIgnored() = assertEquals(TitleInfo("Me at the zoo", null), TitlePreview.parse("NA\tMe at the zoo"))

    @Test fun playlist() = assertEquals(
        TitleInfo("Intro", "Team Fortress 2 [2010 Version]"),
        TitlePreview.parse("Team Fortress 2 [2010 Version]\tIntro\r\n"),
    )

    @Test fun titleWithTabKeepsRest() = assertEquals(TitleInfo("a\tb", null), TitlePreview.parse("\ta\tb"))

    @Test fun noUsableLine() {
        assertNull(TitlePreview.parse(""))
        assertNull(TitlePreview.parse("WARNING: something\n"))
        assertNull(TitlePreview.parse("NA\tNA"))
    }

    @Test fun optionsWithCookiesAndPlaylist() = assertEquals(
        listOf(
            YtOption("--simulate"), YtOption("--flat-playlist"), YtOption("--playlist-items", "1"),
            YtOption("--yes-playlist"), YtOption("--no-warnings"), YtOption("--cookies", "/c.txt"),
            YtOption("--print", "%(playlist_title|)s\t%(title)s"),
        ),
        TitlePreview.options(playlist = true, cookieFile = "/c.txt"),
    )

    @Test fun optionsSingle() =
        assertEquals(YtOption("--no-playlist"), TitlePreview.options(playlist = false, cookieFile = null)[3])
}
