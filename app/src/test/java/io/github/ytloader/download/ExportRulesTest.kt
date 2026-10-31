package io.github.ytloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportRulesTest {
    @Test fun finalFiles() {
        assertTrue(ExportRules.isExportable("a.mp3"))
        assertTrue(ExportRules.isExportable("001 - a.m4a"))
        assertTrue(ExportRules.isExportable("Me at the zoo.mkv"))
        assertTrue(ExportRules.isExportable("a.opus"))
    }

    @Test fun intermediateFiles() {
        assertFalse(ExportRules.isExportable("a.mp3.part"))
        assertFalse(ExportRules.isExportable("a.mp4.ytdl"))
        assertFalse(ExportRules.isExportable("a.temp"))
        assertFalse(ExportRules.isExportable("a.mp4.frag"))
        assertFalse(ExportRules.isExportable("a.mp4.part-Frag12"))
        assertFalse(ExportRules.isExportable("a.f137.mp4"))
        assertFalse(ExportRules.isExportable("a.f251.webm"))
        assertFalse(ExportRules.isExportable("a.temp.mp3"))
    }

    @Test fun interruptedPlaylistItemExcluded() {
        assertTrue(ExportRules.isExportable("001 - a.mp3", interruptedItem = 2))
        assertFalse(ExportRules.isExportable("002 - b.mp3", interruptedItem = 2))
        assertFalse(ExportRules.isExportable("002 - b.webm", interruptedItem = 2))
        assertTrue(ExportRules.isExportable("012 - c.mp3", interruptedItem = 2))
        assertFalse(ExportRules.isExportable("1002 - d.mp3", interruptedItem = 1002))
        assertTrue(ExportRules.isExportable("002 - b.mp3"))
    }

    @Test fun thumbnailLeftovers() {
        assertFalse(ExportRules.isExportable("a.webp"))
        assertFalse(ExportRules.isExportable("a.JPG"))
        assertFalse(ExportRules.isExportable("a.jpeg"))
        assertFalse(ExportRules.isExportable("a.png"))
    }

    @Test fun normalizedNameComposesHangul() {
        val nfd = "아이유 (IU).mp3" // 아이유, 자모 분리형
        assertEquals("아이유 (IU).mp3", ExportRules.normalizedName(nfd))
        assertEquals("Me at the zoo.mp3", ExportRules.normalizedName("Me at the zoo.mp3"))
        assertEquals("아이유", ExportRules.normalizedName("아이유"))
    }
}
