package io.github.dev1ck.ytloader.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException

/** 작업 폴더의 최종 파일 → 공용 Download/YTLoader/<하위 경로> (ARCHITECTURE 8장). */
class MediaStoreExporter(private val context: Context) {

    /** 저장된 URI 목록 (상대 경로 순). 복사 실패 시 IOException. [interruptedItem]은 [ExportRules.isExportable] 참고. */
    fun export(workDir: File, interruptedItem: Int? = null): List<Uri> = workDir.walkTopDown()
        .filter { it.isFile && ExportRules.isExportable(it.name, interruptedItem) }
        .sortedBy { it.relativeTo(workDir).path }
        .map { exportOne(it, workDir) }
        .toList()

    private fun exportOne(file: File, workDir: File): Uri {
        val subDir = file.parentFile!!.relativeTo(workDir).path
        val relativePath = if (subDir.isEmpty()) "$ROOT/" else "$ROOT/$subDir/"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "application/octet-stream"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed: ${file.name}")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("cannot open output: ${file.name}")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw if (e is IOException) e else IOException(e)
        }
        return uri
    }

    companion object {
        const val ROOT = "Download/YTLoader"
    }
}
