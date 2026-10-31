package io.github.dev1ck.ytloader.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {
    @Test fun titleThenUrl() = assertEquals("https://youtu.be/x", UrlExtractor.extract("제목 https://youtu.be/x"))

    @Test fun urlOnly() = assertEquals(
        "https://www.youtube.com/watch?v=jNQXAC9IVRw",
        UrlExtractor.extract("https://www.youtube.com/watch?v=jNQXAC9IVRw"),
    )

    @Test fun firstOfTwo() = assertEquals("http://a.com/1", UrlExtractor.extract("http://a.com/1 https://b.com/2"))

    @Test fun noUrl() {
        assertNull(UrlExtractor.extract("주소 없음"))
        assertNull(UrlExtractor.extract(""))
        assertNull(UrlExtractor.extract(null))
    }

    @Test fun trailingPunctuationRemoved() {
        assertEquals("https://youtu.be/x", UrlExtractor.extract("(https://youtu.be/x)."))
        assertEquals("https://youtu.be/x", UrlExtractor.extract("\"https://youtu.be/x\""))
        assertEquals("https://youtu.be/x?t=1", UrlExtractor.extract("보세요: https://youtu.be/x?t=1!?"))
    }

    @Test fun youtubeHosts() {
        assertEquals(true, UrlExtractor.isYouTube("https://youtu.be/jNQXAC9IVRw"))
        assertEquals(true, UrlExtractor.isYouTube("https://www.youtube.com/watch?v=x"))
        assertEquals(true, UrlExtractor.isYouTube("https://m.youtube.com/watch?v=x"))
        assertEquals(true, UrlExtractor.isYouTube("https://music.youtube.com/playlist?list=y"))
        assertEquals(true, UrlExtractor.isYouTube("HTTPS://YOUTU.BE/x"))
        assertEquals(false, UrlExtractor.isYouTube("https://notyoutube.com/watch?v=x"))
        assertEquals(false, UrlExtractor.isYouTube("https://example.com/?u=https://youtu.be/x"))
        assertEquals(false, UrlExtractor.isYouTube("https://youtube.com.evil.example/x"))
    }

    @Test fun newlineSeparated() =
        assertEquals("https://youtu.be/x", UrlExtractor.extract("제목\nhttps://youtu.be/x\n"))
}
