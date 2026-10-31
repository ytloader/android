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

    @Test fun newlineSeparated() =
        assertEquals("https://youtu.be/x", UrlExtractor.extract("제목\nhttps://youtu.be/x\n"))
}
