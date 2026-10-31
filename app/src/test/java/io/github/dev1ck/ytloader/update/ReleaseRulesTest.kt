package io.github.dev1ck.ytloader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseRulesTest {
    @Test fun parse() {
        assertEquals(listOf(0, 2, 0), ReleaseRules.parse("v0.2.0"))
        assertEquals(listOf(1, 10), ReleaseRules.parse("1.10"))
        assertNull(ReleaseRules.parse("v0.2.0-beta"))
        assertNull(ReleaseRules.parse(""))
    }

    @Test fun isNewer() {
        assertTrue(ReleaseRules.isNewer("v0.2.0", "0.1.0"))
        assertTrue(ReleaseRules.isNewer("v0.10.0", "0.9.9"))
        assertTrue(ReleaseRules.isNewer("v1.0.1", "1.0"))
        assertFalse(ReleaseRules.isNewer("v0.1.0", "0.1.0"))
        assertFalse(ReleaseRules.isNewer("v1.0", "1.0.0"))
        assertFalse(ReleaseRules.isNewer("v0.1.0", "0.2.0"))
        assertFalse(ReleaseRules.isNewer("nightly", "0.1.0"))
    }

    @Test fun apkAsset() {
        assertEquals("YTLoader-0.2.0.apk", ReleaseRules.apkAsset(listOf("notes.txt", "YTLoader-0.2.0.apk")))
        assertNull(ReleaseRules.apkAsset(listOf("source.zip")))
    }
}
