package io.github.ytloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareRulesTest {
    @Test fun folderDocumentId() {
        assertEquals("primary:Download/YTLoader", ShareRules.folderDocumentId(null))
        assertEquals("primary:Download/YTLoader", ShareRules.folderDocumentId(""))
        assertEquals("primary:Download/YTLoader/Team Fortress 2", ShareRules.folderDocumentId("Team Fortress 2"))
        assertEquals("primary:Download/YTLoader/a", ShareRules.folderDocumentId("/a/"))
        assertEquals("primary:Music/YT", ShareRules.folderDocumentId(null, "primary:Music/YT"))
        assertEquals("primary:Music/YT/PL", ShareRules.folderDocumentId("PL", "primary:Music/YT"))
        assertEquals("1A2B-3C4D:PL", ShareRules.folderDocumentId("PL", "1A2B-3C4D:"))
    }

    @Test fun locationLabel() {
        assertEquals("Download/YTLoader", ShareRules.locationLabel(null))
        assertEquals("Download/YTLoader/PL", ShareRules.locationLabel(null, "PL"))
        assertEquals("Music/YT", ShareRules.locationLabel("primary:Music/YT"))
        assertEquals("Music/YT/PL", ShareRules.locationLabel("primary:Music/YT", "PL"))
        assertEquals("내부 저장소", ShareRules.locationLabel("primary:"))
        assertEquals("1A2B-3C4D/Music", ShareRules.locationLabel("1A2B-3C4D:Music"))
        assertEquals("1A2B-3C4D", ShareRules.locationLabel("1A2B-3C4D:"))
        assertEquals("opaque-id", ShareRules.locationLabel("opaque-id"))
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
