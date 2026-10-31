package io.github.dev1ck.ytloader.download

import android.net.Uri

/** 저장한 파일. [name]은 저장소가 정한 실제 이름(같은 이름이면 ` (1)`이 붙음). */
data class SavedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    /** 저장 위치 아래 하위 폴더(재생목록). 없으면 빈 문자열. */
    val subDir: String,
    /** 저장 위치: 사용자가 고른 SAF 트리 URI, 기본 위치(Download/YTLoader)면 null. */
    val folder: String?,
)

sealed interface DownloadState {
    data object Idle : DownloadState
    data object Preparing : DownloadState
    /** [progress]: 0–100. */
    data class Running(val progress: Float, val text: String) : DownloadState
    data class PostProcessing(val text: String) : DownloadState
    data class Succeeded(val files: List<SavedFile>) : DownloadState
    /** [saved]: 실패한 재생목록에서 저장한 완성 항목. [acknowledged]: 오류 대화상자를 사용자가 닫았는지. */
    data class Failed(
        val exitCode: Int?,
        val lastError: String,
        val hint: String,
        val saved: List<SavedFile> = emptyList(),
        val acknowledged: Boolean = false,
    ) : DownloadState {
        val savedCount: Int get() = saved.size
    }
    data class Cancelled(val saved: List<SavedFile>) : DownloadState {
        val savedCount: Int get() = saved.size
    }

    val isActive: Boolean get() = this is Preparing || this is Running || this is PostProcessing

    /** 완료·실패·취소 결과가 남아 있음 (완료 카드 표시). */
    val hasResult: Boolean get() = this is Succeeded || this is Failed || this is Cancelled
}
