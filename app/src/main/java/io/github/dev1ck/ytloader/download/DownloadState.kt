package io.github.dev1ck.ytloader.download

import android.net.Uri

sealed interface DownloadState {
    data object Idle : DownloadState
    data object Preparing : DownloadState
    /** [progress]: 0–100. */
    data class Running(val progress: Float, val text: String) : DownloadState
    data class PostProcessing(val text: String) : DownloadState
    data class Succeeded(val uris: List<Uri>) : DownloadState
    /** [acknowledged]: 오류 대화상자를 사용자가 닫았는지. */
    data class Failed(
        val exitCode: Int?,
        val lastError: String,
        val hint: String,
        val savedCount: Int,
        val acknowledged: Boolean = false,
    ) : DownloadState
    data class Cancelled(val savedCount: Int) : DownloadState

    val isActive: Boolean get() = this is Preparing || this is Running || this is PostProcessing
}
