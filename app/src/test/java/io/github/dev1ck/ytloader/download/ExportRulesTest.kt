package io.github.dev1ck.ytloader.download

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

    @Test fun thumbnailLeftovers() {
        assertFalse(ExportRules.isExportable("a.webp"))
        assertFalse(ExportRules.isExportable("a.JPG"))
        assertFalse(ExportRules.isExportable("a.jpeg"))
        assertFalse(ExportRules.isExportable("a.png"))
    }
}
