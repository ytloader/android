package io.github.dev1ck.ytloader.ui.main

import io.github.dev1ck.ytloader.EngineStatus
import io.github.dev1ck.ytloader.download.DownloadOptions
import io.github.dev1ck.ytloader.download.DownloadState
import io.github.dev1ck.ytloader.download.FormatCatalog
import io.github.dev1ck.ytloader.download.FormatOption
import io.github.dev1ck.ytloader.download.Mode
import io.github.dev1ck.ytloader.download.QualityOption
import io.github.dev1ck.ytloader.login.LoginState
import io.github.dev1ck.ytloader.settings.Settings

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
    val useCookies: Boolean = true,
    val loginState: LoginState = LoginState.LoggedOut,
    val screen: Screen = Screen.Main,
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

    val updateEnabled: Boolean get() = !busy && engineReady
    /** 로그인 영역은 엔진 상태와 무관하게 쓸 수 있다 (작업 중에만 막음). */
    val loginEnabled: Boolean get() = !busy

    fun toOptions() = DownloadOptions(mode, format.value, quality.value, embedThumbnail, embedMetadata, playlist)

    fun applySettings(s: Settings) = copy(
        mode = s.mode, audioFormat = s.audioFormat, audioQuality = s.audioQuality,
        videoFormat = s.videoFormat, videoResolution = s.videoResolution,
        embedThumbnail = s.embedThumbnail, embedMetadata = s.embedMetadata, playlist = s.playlist,
        useCookies = s.useCookies,
    )

    fun toSettings(base: Settings) = base.copy(
        mode = mode, audioFormat = audioFormat, audioQuality = audioQuality,
        videoFormat = videoFormat, videoResolution = videoResolution,
        embedThumbnail = embedThumbnail, embedMetadata = embedMetadata, playlist = playlist,
        useCookies = useCookies,
    )
}

enum class Screen { Main, Login }

sealed interface MainDialog {
    data class EngineFailed(val message: String) : MainDialog
    data class UpdateResult(val message: String) : MainDialog
    data object LogoutConfirm : MainDialog
}
