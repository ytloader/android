package io.github.dev1ck.ytloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareRulesTest {
    @Test fun folderDocumentId() {
        assertEquals("primary:Download/YTLoader", ShareRules.folderDocumentId(null))
        assertEquals("primary:Download/YTLoader", ShareRules.folderDocumentId(""))
        assertEquals("primary:Download/YTLoader/Team Fortress 2", ShareRules.folderDocumentId("Team Fortress 2"))
        assertEquals("primary:Download/YTLoader/a", ShareRules.folderDocumentId("/a/"))
    }

    @Test fun shareMimeType() {
        assertEquals("audio/mpeg", ShareRules.shareMimeType(listOf("audio/mpeg")))
        assertEquals("audio/mpeg", ShareRules.shareMimeType(listOf("audio/mpeg", "audio/mpeg")))
        assertEquals("audio/*", ShareRules.shareMimeType(listOf("audio/mpeg", "audio/ogg")))
        assertEquals("*/*", ShareRules.shareMimeType(listOf("audio/mpeg", "video/mp4")))
        assertEquals("*/*", ShareRules.shareMimeType(emptyList()))
    }

    @Test fun commonSubDir() {
        assertEquals("PL", ShareRules.commonSubDir(listOf("PL", "PL")))
        assertNull(ShareRules.commonSubDir(listOf("", "")))
        assertNull(ShareRules.commonSubDir(listOf("A", "B")))
        assertNull(ShareRules.commonSubDir(emptyList()))
    }
}
