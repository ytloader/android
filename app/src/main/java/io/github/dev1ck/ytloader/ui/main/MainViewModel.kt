package io.github.dev1ck.ytloader.ui.main

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dev1ck.ytloader.EngineStatus
import io.github.dev1ck.ytloader.R
import io.github.dev1ck.ytloader.YTLoaderApp
import io.github.dev1ck.ytloader.download.DownloadRequest
import io.github.dev1ck.ytloader.download.DownloadService
import io.github.dev1ck.ytloader.download.DownloadState
import io.github.dev1ck.ytloader.download.Mode
import io.github.dev1ck.ytloader.engine.UpdateResult
import io.github.dev1ck.ytloader.login.LoginState
import io.github.dev1ck.ytloader.settings.Settings
import io.github.dev1ck.ytloader.util.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as YTLoaderApp).container
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private var engineFailureShown = false

    // 불러오기 전에는 저장하지 않는다 (기본값으로 덮어쓰지 않도록).
    private var settings: Settings? = null

    init {
        viewModelScope.launch {
            val loaded = container.settingsRepository.load()
            settings = loaded
            _state.update { it.applySettings(loaded) }
        }
        viewModelScope.launch {
            container.downloadRepository.state.collect { s ->
                val wasActive = _state.value.downloadState.isActive
                _state.update { it.copy(downloadState = s) }
                // yt-dlp가 실행 후 cookies.txt를 다시 쓰므로 끝나면 로그인 표시를 갱신한다.
                if (wasActive && !s.isActive) container.cookieStore.refresh()
            }
        }
        viewModelScope.launch {
            container.cookieStore.state.collect { s -> _state.update { it.copy(loginState = s) } }
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
    fun setMode(mode: Mode) = edit { it.copy(mode = mode) }

    fun setFormat(value: String) = edit {
        if (it.mode == Mode.AUDIO) it.copy(audioFormat = value) else it.copy(videoFormat = value)
    }

    fun setQuality(value: String) = edit {
        if (it.mode == Mode.AUDIO) it.copy(audioQuality = value) else it.copy(videoResolution = value)
    }

    fun setEmbedThumbnail(checked: Boolean) = edit { it.copy(embedThumbnail = checked) }
    fun setEmbedMetadata(checked: Boolean) = edit { it.copy(embedMetadata = checked) }
    fun setPlaylist(checked: Boolean) = edit { it.copy(playlist = checked) }
    fun setUseCookies(checked: Boolean) = edit { it.copy(useCookies = checked) }

    // ---- 로그인 (FR-9, FR-10) ----

    fun openLogin() {
        if (_state.value.loginEnabled) _state.update { it.copy(screen = Screen.Login) }
    }

    fun onLoginCancelled() {
        _state.update { it.copy(screen = Screen.Main) }
        showMessage(R.string.login_cancelled)
    }

    fun onLoginCookies(cookieHeader: String) {
        viewModelScope.launch {
            container.cookieStore.saveFromWebView(cookieHeader)
            onLoggedIn(R.string.login_done)
        }
    }

    fun importCookies(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching { container.cookieStore.import(uri) }.getOrDefault(false)
            if (ok) onLoggedIn(R.string.cookies_imported) else showMessage(R.string.error_invalid_cookies)
        }
    }

    // 데스크톱판과 같이 로그인하면 [로그인 쿠키 사용]을 켠다.
    private fun onLoggedIn(@StringRes message: Int) {
        _state.update { it.copy(screen = Screen.Main) }
        edit { it.copy(useCookies = true) }
        showMessage(message)
    }

    fun requestLogout() = _state.update { it.copy(dialog = MainDialog.LogoutConfirm) }

    fun confirmLogout() {
        container.cookieStore.logout()
        _state.update { it.copy(dialog = null) }
        showMessage(R.string.logout_done)
    }

    private fun edit(change: (MainUiState) -> MainUiState) {
        val next = _state.updateAndGet(change)
        settings?.let { saveSettings(next.toSettings(it)) }
    }

    private fun saveSettings(next: Settings) {
        settings = next
        viewModelScope.launch { container.settingsRepository.save(next) }
    }

    fun showMessage(message: String) = _state.update { it.copy(message = message) }
    private fun showMessage(@StringRes id: Int) = showMessage(getApplication<Application>().getString(id))
    fun messageShown() = _state.update { it.copy(message = null) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }
    fun acknowledgeFailure() = container.downloadRepository.acknowledgeFailure()

    /** FR-2: 공유 텍스트에서 첫 URL을 꺼내 필드에 넣는다. 자동으로 시작하지 않는다. */
    fun onSharedText(text: String?) {
        val url = UrlExtractor.extract(text)
        if (url == null) {
            showMessage(R.string.error_no_url_in_share)
            return
        }
        _state.update { it.copy(url = url) }
        if (_state.value.downloadState.isActive) showMessage(R.string.message_share_while_busy)
    }

    /** URL이 비어 있으면 false (안내만 표시). */
    fun validateUrl(): Boolean {
        if (_state.value.url.trim().isNotEmpty()) return true
        showMessage(R.string.error_empty_url)
        return false
    }

    fun startDownload() {
        val s = _state.value
        val url = s.url.trim()
        if (url.isEmpty() || !s.downloadEnabled) return
        if (!container.downloadRepository.tryBegin()) return
        val cookieFile = container.cookieStore.file.takeIf { s.loginState is LoginState.LoggedIn && s.useCookies }
        try {
            DownloadService.start(getApplication(), DownloadRequest(url, s.toOptions(), cookieFile?.absolutePath))
        } catch (e: IllegalStateException) {
            // 백그라운드 FGS 시작 제한 등. Preparing에 묶여 있지 않도록 실패로 돌린다.
            container.downloadRepository.set(
                DownloadState.Failed(null, e.message ?: e.javaClass.simpleName, "", savedCount = 0),
            )
        }
    }

    fun cancelDownload() = DownloadService.cancel(getApplication())

    /** 알림 권한을 아직 묻지 않았으면 true를 돌려주고 물은 것으로 저장한다. */
    fun consumeNotificationPermissionAsk(): Boolean {
        val current = settings ?: return false
        if (current.notificationPermissionAsked) return false
        saveSettings(current.copy(notificationPermissionAsked = true))
        return true
    }

    /** FR-8: STABLE 채널 업데이트 후 버전을 다시 읽는다. */
    fun updateYtDlp() {
        if (!_state.value.updateEnabled) return
        _state.update { it.copy(updating = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { container.engine.update() }
            val app = getApplication<Application>()
            val message = when (result) {
                is UpdateResult.Updated -> app.getString(R.string.update_done, result.version.orEmpty())
                is UpdateResult.AlreadyUpToDate -> app.getString(R.string.update_already, result.version.orEmpty())
                is UpdateResult.Failed -> app.getString(R.string.update_failed, result.reason)
            }
            when (result) {
                is UpdateResult.Updated -> container.ytDlpVersion.value = result.version
                is UpdateResult.AlreadyUpToDate -> container.ytDlpVersion.value = result.version
                is UpdateResult.Failed -> Unit
            }
            _state.update { it.copy(updating = false, dialog = MainDialog.UpdateResult(message)) }
        }
    }
}
