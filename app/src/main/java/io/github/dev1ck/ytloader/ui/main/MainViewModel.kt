package io.github.dev1ck.ytloader.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dev1ck.ytloader.EngineStatus
import io.github.dev1ck.ytloader.R
import io.github.dev1ck.ytloader.YTLoaderApp
import io.github.dev1ck.ytloader.download.DownloadRequest
import io.github.dev1ck.ytloader.download.DownloadService
import io.github.dev1ck.ytloader.download.Mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as YTLoaderApp).container
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private var engineFailureShown = false

    init {
        viewModelScope.launch {
            container.downloadRepository.state.collect { s -> _state.update { it.copy(downloadState = s) } }
        }
        viewModelScope.launch {
            container.engineStatus.collect { status ->
                _state.update {
                    val dialog = if (status is EngineStatus.Failed && !engineFailureShown) {
                        engineFailureShown = true
                        MainDialog.EngineFailed(status.message)
                    } else it.dialog
                    it.copy(engineStatus = status, dialog = dialog)
                }
            }
        }
        viewModelScope.launch {
            container.ytDlpVersion.collect { v -> _state.update { it.copy(ytDlpVersion = v) } }
        }
    }

    fun setUrl(url: String) = _state.update { it.copy(url = url) }
    fun setMode(mode: Mode) = _state.update { it.copy(mode = mode) }

    fun setFormat(value: String) = _state.update {
        if (it.mode == Mode.AUDIO) it.copy(audioFormat = value) else it.copy(videoFormat = value)
    }

    fun setQuality(value: String) = _state.update {
        if (it.mode == Mode.AUDIO) it.copy(audioQuality = value) else it.copy(videoResolution = value)
    }

    fun setEmbedThumbnail(checked: Boolean) = _state.update { it.copy(embedThumbnail = checked) }
    fun setEmbedMetadata(checked: Boolean) = _state.update { it.copy(embedMetadata = checked) }
    fun setPlaylist(checked: Boolean) = _state.update { it.copy(playlist = checked) }

    fun showMessage(message: String) = _state.update { it.copy(message = message) }
    fun messageShown() = _state.update { it.copy(message = null) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }
    fun acknowledgeFailure() = container.downloadRepository.acknowledgeFailure()

    /** URL이 비어 있으면 false (안내만 표시). */
    fun validateUrl(): Boolean {
        if (_state.value.url.trim().isNotEmpty()) return true
        showMessage(getApplication<Application>().getString(R.string.error_empty_url))
        return false
    }

    fun startDownload() {
        val s = _state.value
        val url = s.url.trim()
        if (url.isEmpty() || !s.downloadEnabled) return
        if (!container.downloadRepository.tryBegin()) return
        DownloadService.start(getApplication(), DownloadRequest(url, s.toOptions(), cookieFile = null))
    }

    fun cancelDownload() = DownloadService.cancel(getApplication())

    private var notificationPermissionAsked = false

    /** 아직 묻지 않았으면 true를 돌려주고 물은 것으로 기록한다. */
    fun consumeNotificationPermissionAsk(): Boolean {
        if (notificationPermissionAsked) return false
        notificationPermissionAsked = true
        return true
    }
}
