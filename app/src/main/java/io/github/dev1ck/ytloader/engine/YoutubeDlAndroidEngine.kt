package io.github.dev1ck.ytloader.engine

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.dev1ck.ytloader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "YTLoader"

/** youtubedl-android 0.18.1 구현. 라이브러리 동작은 ARCHITECTURE 4장. */
class YoutubeDlAndroidEngine(private val context: Context) : YtDlpEngine {
    private val ytdl get() = YoutubeDL.getInstance()

    override suspend fun init() = withContext(Dispatchers.IO) {
        ytdl.init(context)
        FFmpeg.getInstance().init(context)
    }

    // version(ctx)는 업데이트 전에는 null이라 실제 바이너리에 묻는다.
    override fun version(): String? = try {
        val request = YoutubeDLRequest(emptyList<String>()).addOption("--version")
        ytdl.execute(request, null, null).out.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
    } catch (e: Exception) {
        Log.w(TAG, "yt-dlp --version failed", e)
        null
    }

    override fun execute(
        url: String,
        options: List<YtOption>,
        processId: String,
        onLine: (progress: Float, etaSec: Long, line: String) -> Unit,
    ): ExecResult {
        val request = request(url, options)
        var lastError: String? = null
        return try {
            // stderr도 콜백으로 받아 ERROR: 줄을 놓치지 않는다 (실패 시 예외 메시지는 비어 있음).
            val response = ytdl.execute(request, processId, true) { progress, eta, line ->
                if (line.startsWith("ERROR:")) lastError = line
                onLine(progress, eta, line)
            }
            ExecResult(response.exitCode, lastError, cancelled = false)
        } catch (e: YoutubeDL.CanceledException) {
            ExecResult(null, lastError, cancelled = true)
        } catch (e: InterruptedException) {
            ExecResult(null, lastError, cancelled = true)
        } catch (e: Exception) {
            val fallback = e.message?.lines()?.lastOrNull { it.isNotBlank() } ?: e.javaClass.simpleName
            ExecResult(null, lastError ?: fallback, cancelled = false)
        }
    }

    // stdout만 필요하므로 stderr를 합치지 않는다.
    override fun query(url: String, options: List<YtOption>, processId: String): String? = try {
        ytdl.execute(request(url, options), processId, false, null).out
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.d(TAG, "query failed: ${e.javaClass.simpleName}")
        null
    }

    private fun request(url: String, options: List<YtOption>) = YoutubeDLRequest(url).apply {
        for (option in options) {
            if (option.value == null) addOption(option.name) else addOption(option.name, option.value)
        }
    }

    /**
     * 라이브러리는 yt-dlp(python)만 종료하고 하위 ffmpeg는 남긴다(ARCHITECTURE 4장).
     * 작업 폴더 경로에 [processId]가 들어 있으므로, 명령줄에 그 값이 있는 자기 UID 프로세스를 함께 종료한다.
     */
    override fun cancel(processId: String): Boolean {
        val destroyed = ytdl.destroyProcessById(processId)
        killProcessesMentioning(processId)
        return destroyed
    }

    private fun killProcessesMentioning(marker: String) {
        val self = android.os.Process.myPid()
        File("/proc").listFiles()?.forEach { dir ->
            val pid = dir.name.toIntOrNull() ?: return@forEach
            if (pid == self) return@forEach
            val cmdline = runCatching { File(dir, "cmdline").readText() }.getOrNull() ?: return@forEach
            if (marker in cmdline) {
                Log.i(TAG, "killing child process $pid")
                android.os.Process.killProcess(pid)
            }
        }
    }

    override fun update(): UpdateResult = try {
        when (ytdl.updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)) {
            YoutubeDL.UpdateStatus.DONE -> UpdateResult.Updated(version())
            YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> UpdateResult.AlreadyUpToDate(version())
            null -> UpdateResult.Failed("알 수 없는 응답")
        }
    } catch (e: Exception) {
        Log.w(TAG, "yt-dlp update failed", e)
        UpdateResult.Failed(listOfNotNull(e.message, e.cause?.message).joinToString(": ").ifEmpty { e.javaClass.simpleName })
    }
}
