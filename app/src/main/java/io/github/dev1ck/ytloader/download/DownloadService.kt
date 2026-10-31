package io.github.dev1ck.ytloader.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import io.github.dev1ck.ytloader.BuildConfig
import io.github.dev1ck.ytloader.R
import io.github.dev1ck.ytloader.YTLoaderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.UUID

private const val TAG = "YTLoader"

@Parcelize
data class DownloadRequest(val url: String, val options: DownloadOptions, val cookieFile: String?) : Parcelable

/** 포그라운드 서비스(dataSync)에서 yt-dlp를 실행한다 (ARCHITECTURE 7장). */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val container get() = (application as YTLoaderApp).container
    private lateinit var notifications: DownloadNotifications
    private var job: Job? = null

    @Volatile private var processId: String? = null
    @Volatile private var cancelRequested = false
    @Volatile private var timedOut = false

    override fun onCreate() {
        super.onCreate()
        notifications = DownloadNotifications(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // startForegroundService 뒤에는 무시하는 경우에도 startForeground를 불러야 한다.
                goForeground(getString(R.string.status_preparing), null)
                val request = IntentCompat.getParcelableExtra(intent, EXTRA_REQUEST, DownloadRequest::class.java)
                if (job?.isActive == true || request == null) return START_NOT_STICKY
                cancelRequested = false
                timedOut = false
                job = scope.launch { run(request) }
            }
            ACTION_CANCEL -> {
                if (job?.isActive == true) requestCancel() else stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15+ dataSync 시간 제한. 수 초 안에 stopSelf 하지 않으면 앱이 종료된다.
        Log.w(TAG, "foreground service timeout")
        timedOut = true
        requestCancel()
        container.downloadRepository.set(
            DownloadState.Failed(null, getString(R.string.error_timeout), ""),
        )
        notifications.showFailed(getString(R.string.error_timeout))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        processId?.let { container.engine.cancel(it) }
        scope.cancel()
        super.onDestroy()
    }

    private fun requestCancel() {
        cancelRequested = true
        processId?.let { container.engine.cancel(it) }
    }

    private fun run(request: DownloadRequest) {
        val repository = container.downloadRepository
        val jobId = UUID.randomUUID().toString()
        val workDir = File(container.workRoot, jobId)
        var progress = 0f
        var currentItem: Int? = null
        var lastNotify = 0L
        try {
            repository.set(DownloadState.Preparing)
            workDir.mkdirs()
            val args = YtDlpArgsBuilder.build(request.options, workDir.absolutePath, request.cookieFile)
            processId = jobId
            val result = container.engine.execute(request.url, args, jobId) { _, _, line ->
                if (BuildConfig.DEBUG) Log.d(TAG, line)
                if (cancelRequested) container.engine.cancel(jobId)
                val state = when (val parsed = ProgressParser.parse(line)) {
                    is ParsedLine.Progress -> DownloadState.Running(parsed.percent.also { progress = it }, parsed.text)
                    is ParsedLine.PlaylistItem -> DownloadState.Running(progress, parsed.text).also { currentItem = parsed.index }
                    is ParsedLine.PostProcessing -> DownloadState.PostProcessing(parsed.text).also { progress = 100f }
                    is ParsedLine.Error, null -> null
                } ?: return@execute
                repository.set(state)
                val now = SystemClock.elapsedRealtime()
                if (now - lastNotify >= NOTIFY_INTERVAL_MS) {
                    lastNotify = now
                    notifications.notifyProgress(statusText(state), progress)
                }
            }
            processId = null
            if (timedOut) return

            val playlist = request.options.playlist
            val final = when {
                result.cancelled || (cancelRequested && !result.succeeded) ->
                    DownloadState.Cancelled(if (playlist) exportQuietly(workDir, currentItem) else emptyList())
                result.succeeded -> try {
                    DownloadState.Succeeded(MediaStoreExporter(this).export(workDir))
                } catch (e: Exception) {
                    Log.e(TAG, "export failed", e)
                    val message = getString(R.string.error_export, e.message ?: e.javaClass.simpleName)
                    DownloadState.Failed(null, message, "")
                }
                else -> {
                    val error = result.lastError ?: getString(R.string.error_unknown)
                    val saved = if (playlist) exportQuietly(workDir, currentItem) else emptyList()
                    DownloadState.Failed(result.exitCode, error, ErrorHintMapper.hint(error), saved)
                }
            }
            repository.set(final)
            when (final) {
                is DownloadState.Succeeded -> notifications.showSucceeded(final.files)
                is DownloadState.Failed -> notifications.showFailed(final.lastError, final.saved)
                else -> Unit
            }
        } finally {
            processId = null
            workDir.deleteRecursively()
            if (!timedOut) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /** 실패·취소한 재생목록에서 완성된 항목만 저장한다 (처리 중이던 [interruptedItem] 제외). */
    private fun exportQuietly(workDir: File, interruptedItem: Int?): List<SavedFile> = try {
        MediaStoreExporter(this).export(workDir, interruptedItem)
    } catch (e: Exception) {
        Log.e(TAG, "partial export failed", e)
        emptyList()
    }

    private fun statusText(state: DownloadState): String = when (state) {
        is DownloadState.Running -> state.text
        is DownloadState.PostProcessing -> state.text
        else -> getString(R.string.status_preparing)
    }

    private fun goForeground(text: String, progress: Float?) {
        ServiceCompat.startForeground(
            this, DownloadNotifications.ID_PROGRESS, notifications.progress(text, progress),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        const val ACTION_START = "io.github.dev1ck.ytloader.action.START"
        const val ACTION_CANCEL = "io.github.dev1ck.ytloader.action.CANCEL"
        private const val EXTRA_REQUEST = "request"
        private const val NOTIFY_INTERVAL_MS = 500L

        fun start(context: Context, request: DownloadRequest) {
            val intent = Intent(context, DownloadService::class.java).setAction(ACTION_START).putExtra(EXTRA_REQUEST, request)
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            context.startService(Intent(context, DownloadService::class.java).setAction(ACTION_CANCEL))
        }
    }
}
