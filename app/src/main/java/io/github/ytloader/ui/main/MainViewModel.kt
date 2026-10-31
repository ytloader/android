package io.github.ytloader.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.ytloader.BuildConfig
import io.github.ytloader.EngineStatus
import io.github.ytloader.R
import io.github.ytloader.YTLoaderApp
import io.github.ytloader.download.DownloadRequest
import io.github.ytloader.download.DownloadService
import io.github.ytloader.download.DownloadState
import io.github.ytloader.download.FileActions
import io.github.ytloader.download.Mode
import io.github.ytloader.engine.UpdateResult
import io.github.ytloader.login.LoginState
import io.github.ytloader.settings.Settings
import io.github.ytloader.update.AppUpdateState
import io.github.ytloader.update.ReleaseRules
import io.github.ytloader.util.UrlExtractor
import io.github.ytloader.download.TitlePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TITLE_DEBOUNCE_MS = 700L
private const val TAG = "YTLoader"

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
            // 사용자가 폴더를 지웠거나 권한을 거둬 갔으면 기본 위치로 되돌린다.
            if (loaded.saveFolder != null && !hasFolderPermission(loaded.saveFolder)) {
                edit { it.copy(saveFolder = null) }
                showMessage(R.string.error_storage_lost)
            }
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
        viewModelScope.launch {
            container.engineUpdating.collect { v -> _state.update { it.copy(engineUpdating = v) } }
        }
        viewModelScope.launch {
            container.appUpdate.collect { v -> _state.update { it.copy(appUpdate = v) } }
        }
        checkAppUpdate()
    }

    fun setUrl(url: String) {
        _state.update { it.copy(url = url) }
        scheduleTitlePreview()
    }

    fun setMode(mode: Mode) = edit { it.copy(mode = mode) }

    fun setFormat(value: String) = edit {
        if (it.mode == Mode.AUDIO) it.copy(audioFormat = value) else it.copy(videoFormat = value)
    }

    fun setQuality(value: String) = edit {
        if (it.mode == Mode.AUDIO) it.copy(audioQuality = value) else it.copy(videoResolution = value)
    }

    fun setEmbedThumbnail(checked: Boolean) = edit { it.copy(embedThumbnail = checked) }
    fun setEmbedMetadata(checked: Boolean) = edit { it.copy(embedMetadata = checked) }
    fun setPlaylist(checked: Boolean) {
        edit { it.copy(playlist = checked) }
        scheduleTitlePreview() // 재생목록 이름 표시 여부가 바뀐다
    }

    fun setUseCookies(checked: Boolean) = edit { it.copy(useCookies = checked) }

    // ---- 저장 위치 ----

    /** 폴더 선택기에서 고른 폴더. null이면 기본 위치. 고른 폴더는 앱을 다시 켜도 쓸 수 있게 영구 권한을 받는다. */
    fun setSaveFolder(tree: Uri?) {
        val resolver = getApplication<Application>().contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val previous = _state.value.saveFolder
        if (tree != null) {
            try {
                resolver.takePersistableUriPermission(tree, flags)
            } catch (e: SecurityException) {
                showMessage(R.string.error_storage_lost)
                return
            }
        }
        if (previous != null && previous != tree?.toString()) {
            runCatching { resolver.releasePersistableUriPermission(previous.toUri(), flags) }
        }
        edit { it.copy(saveFolder = tree?.toString()) }
        showMessage(getApplication<Application>().getString(R.string.storage_changed, FileActions.locationLabel(tree?.toString())))
    }

    private fun hasFolderPermission(folder: String): Boolean =
        getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri.toString() == folder && it.isWritePermission
        }

    // ---- 제목 미리보기 ----

    private var titleJob: Job? = null
    @Volatile private var titleProcessId: String? = null

    /** 입력이 멈추고 잠시 뒤 제목을 조회한다. 새 입력이 오면 이전 조회는 취소. */
    private fun scheduleTitlePreview() {
        stopTitlePreview()
        val url = _state.value.url.trim()
        if (UrlExtractor.extract(url) != url) {
            _state.update { it.copy(titlePreview = TitleState.Idle) }
            return
        }
        titleJob = viewModelScope.launch {
            delay(TITLE_DEBOUNCE_MS)
            if (container.engineStatus.first { it != EngineStatus.Initializing } != EngineStatus.Ready) return@launch
            val s = _state.value
            if (s.downloadState.isActive) return@launch
            _state.update { it.copy(titlePreview = TitleState.Loading) }
            val id = "title-" + UUID.randomUUID()
            titleProcessId = id
            val options = TitlePreview.options(s.playlist, cookieFileFor(s)?.absolutePath)
            val info = withContext(Dispatchers.IO) { container.engine.query(url, options, id) }?.let(TitlePreview::parse)
            titleProcessId = null
            _state.update {
                if (it.url.trim() != url) it
                else it.copy(titlePreview = info?.let(TitleState::Loaded) ?: TitleState.Failed)
            }
        }
    }

    private fun stopTitlePreview() {
        titleJob?.cancel()
        titleJob = null
        titleProcessId?.let { id -> viewModelScope.launch(Dispatchers.IO) { container.engine.cancel(id) } }
        titleProcessId = null
        _state.update { if (it.titlePreview == TitleState.Loading) it.copy(titlePreview = TitleState.Idle) else it }
    }

    // ---- 클립보드 제안 ----

    private var dismissedSuggestion: String? = null

    /** 화면이 포커스를 얻을 때 읽은 클립보드 텍스트. 입력란이 비어 있을 때만 YouTube 링크를 제안한다. */
    fun onClipboardText(text: String?) {
        val url = UrlExtractor.extract(text)
        val suggest = url?.takeIf {
            UrlExtractor.isYouTube(it) && it != dismissedSuggestion && _state.value.url.isBlank()
        }
        _state.update { it.copy(clipboardSuggestion = suggest) }
    }

    fun acceptClipboardSuggestion() {
        val url = _state.value.clipboardSuggestion ?: return
        dismissedSuggestion = url
        _state.update { it.copy(clipboardSuggestion = null) }
        setUrl(url)
    }

    fun dismissClipboardSuggestion() {
        dismissedSuggestion = _state.value.clipboardSuggestion
        _state.update { it.copy(clipboardSuggestion = null) }
    }

    // ---- 앱 자체 업데이트 (FR-15) ----

    /** 프로세스마다 한 번 GitHub 최신 릴리스를 확인한다. 실패하면 조용히 넘어간다. debug 빌드는 서명이 달라 건너뜀. */
    private fun checkAppUpdate() {
        if (BuildConfig.DEBUG || container.appUpdate.value != AppUpdateState.None) return
        viewModelScope.launch(Dispatchers.IO) {
            val release = runCatching { container.appUpdater.latest() }
                .onFailure { Log.w(TAG, "app update check failed: ${it.javaClass.simpleName}") }
                .getOrNull() ?: return@launch
            if (ReleaseRules.isNewer(release.version, BuildConfig.VERSION_NAME)) {
                container.appUpdate.compareAndSet(AppUpdateState.None, AppUpdateState.Available(release))
            }
        }
    }

    /** [업데이트]: 받고 바로 설치한다. 확인 화면이 필요하면 시스템이 띄운다. */
    fun startAppUpdate() {
        val release = when (val s = container.appUpdate.value) {
            is AppUpdateState.Available -> s.release
            is AppUpdateState.Failed -> s.release
            else -> return
        }
        if (_state.value.busy) return showMessage(R.string.app_update_busy)
        container.appUpdate.value = AppUpdateState.Downloading(release, null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apk = container.appUpdater.download(release) { p ->
                    container.appUpdate.value = AppUpdateState.Downloading(release, p)
                }
                container.appUpdate.value = AppUpdateState.Installing(release)
                container.appUpdater.install(apk)
            } catch (e: Exception) {
                Log.w(TAG, "app update failed", e)
                container.appUpdater.clearDownloads()
                val reason = e.message ?: e.javaClass.simpleName
                container.appUpdate.value = AppUpdateState.Failed(
                    release, getApplication<Application>().getString(R.string.app_update_failed, reason),
                )
            }
        }
    }

    /** 배너 ✕: 이번 실행 동안 숨긴다. */
    fun dismissAppUpdate() {
        if (!_state.value.appUpdateInProgress) container.appUpdate.value = AppUpdateState.Dismissed
    }

    fun showFailureDetail() = container.downloadRepository.reopenFailure()

    /** 완료 카드의 ✕: 결과 표시를 지운다. */
    fun dismissResult() = container.downloadRepository.clearResult()

    private fun cookieFileFor(s: MainUiState) =
        container.cookieStore.file.takeIf { s.loginState is LoginState.LoggedIn && s.useCookies }

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
        setUrl(url)
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
        stopTitlePreview() // 동시에 yt-dlp를 두 개 돌리지 않는다
        val cookieFile = cookieFileFor(s)
        try {
            DownloadService.start(getApplication(), DownloadRequest(url, s.toOptions(), cookieFile?.absolutePath, s.saveFolder))
        } catch (e: IllegalStateException) {
            // 백그라운드 FGS 시작 제한 등. Preparing에 묶여 있지 않도록 실패로 돌린다.
            container.downloadRepository.set(
                DownloadState.Failed(null, e.message ?: e.javaClass.simpleName, ""),
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
