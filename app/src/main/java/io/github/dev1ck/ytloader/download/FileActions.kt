package io.github.dev1ck.ytloader.download

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri

/** 저장한 파일 열기·공유·폴더 열기 Intent (FR-12). 화면과 완료 알림이 함께 쓴다. */
object FileActions {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun openIntent(file: SavedFile): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(file.uri, file.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** 시스템 공유 시트. 받는 앱에는 이 파일들의 읽기 권한만 준다. */
    fun shareIntent(files: List<SavedFile>, title: String): Intent {
        val send = if (files.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, files[0].uri)
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files.map { it.uri }))
        }
        send.type = ShareRules.shareMimeType(files.map { it.mimeType })
        send.clipData = ClipData.newRawUri(null, files[0].uri).apply { files.drop(1).forEach { addItem(ClipData.Item(it.uri)) } }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * 파일 앱에서 저장 폴더를 연다. 시스템 파일 앱(DocumentsUI)이 있으면 그 앱을 직접 지정한다 —
     * 삼성 기기에서는 상관없는 앱(예: DJI Ronin)도 폴더 열기를 받아 선택 창이 뜨기 때문이다.
     * 폴더를 받는 앱이 없으면 다운로드 화면, 그것도 없으면 null.
     */
    fun folderIntent(context: Context, folder: String?, subDir: String?): Intent? {
        val pm = context.packageManager
        val folderUri = if (folder == null) {
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, ShareRules.folderDocumentId(subDir))
        } else {
            val tree = folder.toUri()
            DocumentsContract.buildDocumentUri(tree.authority, ShareRules.folderDocumentId(subDir, DocumentsContract.getTreeDocumentId(tree)))
        }
        val folder = Intent(Intent.ACTION_VIEW).setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
        val handlers = pm.queryIntentActivities(folder, 0).map { it.activityInfo.packageName }
        handlers.firstOrNull { it.endsWith(".documentsui") }?.let { return folder.setPackage(it) }
        if (handlers.isNotEmpty()) return folder
        return Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).takeIf { it.resolveActivity(pm) != null }
    }

    /** 저장한 파일들이 모인 폴더를 파일 앱에서 연다. */
    fun folderIntent(context: Context, files: List<SavedFile>): Intent? =
        folderIntent(context, files.firstOrNull()?.folder, ShareRules.commonSubDir(files.map { it.subDir }))

    /** 화면에 보일 저장 위치 (`Download/YTLoader`, `Music/YT/<재생목록>` 등). */
    fun locationLabel(folder: String?, subDir: String? = null): String =
        ShareRules.locationLabel(folder?.let { DocumentsContract.getTreeDocumentId(it.toUri()) }, subDir)

    /** 사용자가 지우지 않고 아직 남아 있는 파일만. */
    fun existing(context: Context, files: List<SavedFile>): List<SavedFile> = files.filter { file ->
        runCatching {
            context.contentResolver.query(file.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }
}
