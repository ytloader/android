package io.github.dev1ck.ytloader.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

/** 작업 폴더의 최종 파일을 저장 위치로 옮긴다 (ARCHITECTURE 8장). */
abstract class Exporter(protected val context: Context) {

    /** 저장한 파일 목록 (상대 경로 순). 복사 실패 시 IOException. [interruptedItem]은 [ExportRules.isExportable] 참고. */
    fun export(workDir: File, interruptedItem: Int? = null): List<SavedFile> = workDir.walkTopDown()
        .filter { it.isFile && ExportRules.isExportable(it.name, interruptedItem) }
        .sortedBy { it.relativeTo(workDir).path }
        .map { file ->
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
                ?: "application/octet-stream"
            val subDir = ExportRules.normalizedName(file.parentFile!!.relativeTo(workDir).path)
            exportOne(file, ExportRules.normalizedName(file.name), subDir, mime)
        }
        .toList()

    /** [name]·[subDir]는 완성형으로 정규화된 저장 이름 ([ExportRules.normalizedName]). */
    protected abstract fun exportOne(file: File, name: String, subDir: String, mime: String): SavedFile

    // 같은 이름이 있으면 MediaStore·문서 제공자가 ` (1)`을 붙이므로 실제 이름을 다시 읽는다.
    protected fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    companion object {
        /** [folder]: 사용자가 고른 SAF 트리 URI. null이면 기본 위치(MediaStore). */
        fun create(context: Context, folder: String?): Exporter =
            if (folder == null) MediaStoreExporter(context) else DocumentTreeExporter(context, folder.toUri())
    }
}

/** 기본 위치: 공용 Download/YTLoader/<하위 경로>. */
class MediaStoreExporter(context: Context) : Exporter(context) {

    override fun exportOne(file: File, name: String, subDir: String, mime: String): SavedFile {
        val relativePath = if (subDir.isEmpty()) "$ROOT/" else "$ROOT/$subDir/"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed: $name")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("cannot open output: $name")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw if (e is IOException) e else IOException(e)
        }
        return SavedFile(uri, displayName(uri) ?: name, file.length(), mime, subDir, folder = null)
    }

    companion object {
        const val ROOT = ShareRules.DEFAULT_LOCATION
    }
}

/** 사용자가 고른 폴더(SAF 트리) 아래 <하위 경로>. 권한은 설정에서 고를 때 영구 권한으로 받아 둔다. */
class DocumentTreeExporter(context: Context, private val tree: Uri) : Exporter(context) {
    private val resolver get() = context.contentResolver
    private val root by lazy { DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)) }

    override fun exportOne(file: File, name: String, subDir: String, mime: String): SavedFile {
        val parent = subDir.split('/').filter { it.isNotEmpty() }.fold(root, ::childDirectory)
        val uri = DocumentsContract.createDocument(resolver, parent, mime, name)
            ?: throw IOException("cannot create document: $name")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("cannot open output: $name")
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            throw if (e is IOException) e else IOException(e)
        }
        return SavedFile(uri, displayName(uri) ?: name, file.length(), mime, subDir, folder = tree.toString())
    }

    /** [dir] 아래 [name] 폴더. 이미 있으면 그대로 쓴다 (기본 위치와 같이 같은 재생목록은 한 폴더에 모인다). */
    private fun childDirectory(dir: Uri, name: String): Uri {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir))
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        resolver.query(children, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name && c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                }
            }
        }
        return DocumentsContract.createDocument(resolver, dir, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: throw IOException("cannot create folder: $name")
    }
}
