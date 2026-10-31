package io.github.dev1ck.ytloader.ui.main

import io.github.dev1ck.ytloader.EngineStatus
import io.github.dev1ck.ytloader.download.DownloadOptions
import io.github.dev1ck.ytloader.download.DownloadState
import io.github.dev1ck.ytloader.download.FormatCatalog
import io.github.dev1ck.ytloader.download.FormatOption
import io.github.dev1ck.ytloader.download.Mode
import io.github.dev1ck.ytloader.download.QualityOption

/** 모드별로 마지막 포맷·품질을 따로 기억한다 (FR-3). */
data class MainUiState(
    val url: String = "",
    val mode: Mode = Mode.AUDIO,
    val audioFormat: String = FormatCatalog.audioFormats.first().value,
    val audioQuality: String = FormatCatalog.audioQualities.first().value,
    val videoFormat: String = FormatCatalog.videoFormats.first().value,
    val videoResolution: String = FormatCatalog.videoResolutions.first().value,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val playlist: Boolean = false,
    val engineStatus: EngineStatus = EngineStatus.Initializing,
    val downloadState: DownloadState = DownloadState.Idle,
    val ytDlpVersion: String? = null,
    val updating: Boolean = false,
    val dialog: MainDialog? = null,
    val message: String? = null,
) {
    val format: FormatOption
        get() = FormatCatalog.format(mode, if (mode == Mode.AUDIO) audioFormat else videoFormat)
    val quality: QualityOption
        get() = FormatCatalog.quality(mode, if (mode == Mode.AUDIO) audioQuality else videoResolution)

    val busy: Boolean get() = downloadState.isActive || updating
    val inputsEnabled: Boolean get() = !busy
    val qualityEnabled: Boolean get() = !busy && format.hasQuality
    val thumbnailEnabled: Boolean get() = !busy && format.supportsThumbnail
    val engineReady: Boolean get() = engineStatus == EngineStatus.Ready
    val downloadEnabled: Boolean get() = !busy && engineReady
    val cancelEnabled: Boolean get() = downloadState.isActive

    fun toOptions() = DownloadOptions(mode, format.value, quality.value, embedThumbnail, embedMetadata, playlist)
}

sealed interface MainDialog {
    data class EngineFailed(val message: String) : MainDialog
}
