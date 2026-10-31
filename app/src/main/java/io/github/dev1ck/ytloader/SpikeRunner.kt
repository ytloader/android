package io.github.dev1ck.ytloader

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

// M1 임시 측정 코드. M3에서 정식 구조로 대체하고 삭제한다.
object SpikeRunner {
    private const val TAG = "YTLoader"
    private const val URL = "https://www.youtube.com/watch?v=jNQXAC9IVRw"

    fun run(context: Context, step: String) {
        val t0 = SystemClock.elapsedRealtime()
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        Log.i(TAG, "SPIKE init ms=${SystemClock.elapsedRealtime() - t0}")
        Log.i(TAG, "SPIKE version(ctx)=${YoutubeDL.getInstance().version(context)} versionName=${YoutubeDL.getInstance().versionName(context)}")
        val work = File(context.filesDir, "spike").apply { deleteRecursively(); mkdirs() }
        val cache = File(context.filesDir, "spike-cache")
        when (step) {
            "init" -> Unit
            "version" -> exec("version", YoutubeDLRequest(emptyList()).addOption("--version"))
            "title" -> exec("title", YoutubeDLRequest(URL).addOption("--simulate").addOption("--print", "title"))
            "title-cache" -> exec(
                "title-cache",
                YoutubeDLRequest(URL).addOption("--simulate").addOption("--print", "title")
                    .addOption("--cache-dir", cache.absolutePath),
            )
            "mp3" -> {
                exec(
                    "mp3",
                    YoutubeDLRequest(URL)
                        .addOption("-f", "bestaudio/best").addOption("-x")
                        .addOption("--audio-format", "mp3").addOption("--audio-quality", "0")
                        .addOption("--embed-thumbnail").addOption("--embed-metadata")
                        .addOption("--no-playlist").addOption("--windows-filenames")
                        .addOption("--newline").addOption("--progress")
                        .addOption("-o", "${work.absolutePath}/%(title)s.%(ext)s"),
                )
                work.walkTopDown().filter { it.isFile }.forEach { Log.i(TAG, "SPIKE file ${it.name} bytes=${it.length()}") }
            }
            "update" -> {
                val t1 = SystemClock.elapsedRealtime()
                try {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                    Log.i(TAG, "SPIKE update ok ms=${SystemClock.elapsedRealtime() - t1} status=$status version(ctx)=${YoutubeDL.getInstance().version(context)}")
                } catch (e: Exception) {
                    Log.i(TAG, "SPIKE update failed ${e.javaClass.simpleName}: ${e.message} / ${e.cause}")
                }
            }
            "fail" -> exec("fail", YoutubeDLRequest("https://www.youtube.com/watch?v=xxxxxxxxxxx").addOption("--simulate"))
            "fail-redirect" -> exec(
                "fail-redirect",
                YoutubeDLRequest("https://www.youtube.com/watch?v=xxxxxxxxxxx").addOption("--simulate"),
                redirect = true,
            )
        }
    }

    private fun exec(label: String, request: YoutubeDLRequest, redirect: Boolean = false) {
        val t0 = SystemClock.elapsedRealtime()
        try {
            val r = YoutubeDL.getInstance().execute(request, "spike-$label", redirect) { p, eta, line ->
                Log.i(TAG, "SPIKE line p=$p eta=$eta | $line")
            }
            Log.i(TAG, "SPIKE $label ok ms=${SystemClock.elapsedRealtime() - t0} exit=${r.exitCode}")
            r.out.lines().filter { it.isNotBlank() }.forEach { Log.i(TAG, "SPIKE out | $it") }
            r.err.lines().filter { it.isNotBlank() }.forEach { Log.i(TAG, "SPIKE err | $it") }
        } catch (e: Exception) {
            Log.i(TAG, "SPIKE $label failed ms=${SystemClock.elapsedRealtime() - t0} ${e.javaClass.simpleName}")
            (e.message ?: "").lines().filter { it.isNotBlank() }.forEach { Log.i(TAG, "SPIKE exc | $it") }
        }
    }
}
