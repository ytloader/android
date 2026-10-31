package io.github.ytloader.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetscapeCookieWriterTest {
    private val lines get() = NetscapeCookieWriter.write("SID=abc; PREF=f1=5000&tz=Asia.Seoul; =orphan; YSC=x==", 1_800_000_000).lines()

    @Test fun header() = assertEquals("# Netscape HTTP Cookie File", lines.first())

    @Test fun sevenTabFields() {
        val sid = lines[1].split('\t')
        assertEquals(listOf(".youtube.com", "TRUE", "/", "TRUE", "1800000000", "SID", "abc"), sid)
    }

    @Test fun valueKeepsEquals() {
        assertEquals("f1=5000&tz=Asia.Seoul", lines[2].split('\t')[6])
        assertEquals("x==", lines[3].split('\t')[6])
    }

    @Test fun emptyNameDropped() {
        val body = lines.drop(1).filter { it.isNotEmpty() }
        assertEquals(listOf("SID", "PREF", "YSC"), body.map { it.split('\t')[5] })
    }

    @Test fun writtenFileIsValidForImport() =
        assertTrue(CookieFileValidator.isValid(NetscapeCookieWriter.write("SAPISID=v", 1)))

    @Test fun namesOnly() = assertEquals(
        listOf("SID", "SAPISID", "__Secure-3PAPISID"),
        NetscapeCookieWriter.names("SID=a; SAPISID=b=c; __Secure-3PAPISID=d"),
    )
}

class CookieFileValidatorTest {
    @Test fun desktopFormatPasses() {
        // 데스크톱판 Save-NetscapeCookies 출력 형식 (CRLF)
        val text = "# Netscape HTTP Cookie File\r\n" +
            ".google.com\tTRUE\t/\tTRUE\t1800000000\tSID\tv\r\n" +
            ".youtube.com\tTRUE\t/\tTRUE\t1800000000\tSAPISID\tv\r\n"
        assertTrue(CookieFileValidator.isValid(text))
    }

    @Test fun httpOnlyLinePasses() =
        assertTrue(CookieFileValidator.isValid("#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tLOGIN_INFO\tv\n"))

    @Test fun withoutYoutubeRejected() =
        assertFalse(CookieFileValidator.isValid("# Netscape HTTP Cookie File\n.google.com\tTRUE\t/\tTRUE\t0\tSID\tv\n"))

    @Test fun lookalikeDomainRejected() =
        assertFalse(CookieFileValidator.isValid("notyoutube.com\tTRUE\t/\tTRUE\t0\tSID\tv\n"))

    @Test fun emptyRejected() {
        assertFalse(CookieFileValidator.isValid(""))
        assertFalse(CookieFileValidator.isValid("# Netscape HTTP Cookie File\n"))
    }

    @Test fun wrongFieldCountRejected() =
        assertFalse(CookieFileValidator.isValid(".youtube.com TRUE / TRUE 0 SID v\n"))
}
