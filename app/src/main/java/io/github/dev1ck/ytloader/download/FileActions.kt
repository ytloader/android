package io.github.dev1ck.ytloader.download

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract

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
     * 파일 앱에서 저장 폴더를 연다. 시스템 파일 앱(DocumentsUI)이 폴더 문서 URI를 받으면 그 폴더로,
     * 받는 앱이 없으면 다운로드 화면으로. 둘 다 없으면 null.
     */
    fun folderIntent(context: Context, subDir: String?): Intent? {
        val pm = context.packageManager
        val folderUri = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, ShareRules.folderDocumentId(subDir))
        val folder = Intent(Intent.ACTION_VIEW).setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
        if (folder.resolveActivity(pm) != null) return folder
        return Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).takeIf { it.resolveActivity(pm) != null }
    }

    /** 사용자가 지우지 않고 아직 남아 있는 파일만. */
    fun existing(context: Context, files: List<SavedFile>): List<SavedFile> = files.filter { file ->
        runCatching {
            context.contentResolver.query(file.uri, arrayOf(android.provider.MediaStore.MediaColumns._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }
}
