package io.github.ytloader.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(val version: String, val apkUrl: String, val size: Long)

sealed interface AppUpdateState {
    data object None : AppUpdateState
    /** 사용자가 배너를 닫음 (이번 실행 동안 다시 확인하지 않음). */
    data object Dismissed : AppUpdateState
    data class Available(val release: AppRelease) : AppUpdateState
    /** [progress]: 0–1, 크기를 모르면 null. */
    data class Downloading(val release: AppRelease, val progress: Float?) : AppUpdateState
    data class Installing(val release: AppRelease) : AppUpdateState
    data class Failed(val release: AppRelease, val message: String) : AppUpdateState
}

/**
 * GitHub 릴리스로 앱 자체를 업데이트한다 (FR-15). 최신 릴리스 확인 → APK 받기 → PackageInstaller 세션.
 * Android 12+에서는 자기 자신을 업데이트할 때 사용자 확인 없이 설치될 수 있고(USER_ACTION_NOT_REQUIRED),
 * 확인이 필요하면 [InstallResultReceiver]가 시스템 설치 화면을 띄운다.
 */
class AppUpdater(private val context: Context) {
    private val updateDir get() = File(context.cacheDir, "update")

    /** 정식 릴리스(사전 릴리스 제외) 중 최신. APK가 없으면 null. 블로킹. */
    fun latest(): AppRelease? {
        val connection = open(LATEST_RELEASE_URL)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        val json = try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
        val assets = json.optJSONArray("assets") ?: return null
        val byName = (0 until assets.length()).map { assets.getJSONObject(it) }.associateBy { it.optString("name") }
        val asset = ReleaseRules.apkAsset(byName.keys.toList())?.let(byName::get) ?: return null
        return AppRelease(json.optString("tag_name"), asset.optString("browser_download_url"), asset.optLong("size"))
    }

    /** APK를 앱 캐시에 받는다. 블로킹. */
    fun download(release: AppRelease, onProgress: (Float?) -> Unit): File {
        updateDir.mkdirs()
        val file = File(updateDir, "YTLoader-${release.version}.apk")
        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.size.takeIf { it > 0 }
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastReported = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        val percent = total?.let { (done * 100 / it).toInt() } ?: -1
                        if (percent != lastReported) {
                            lastReported = percent
                            onProgress(total?.let { done.toFloat() / it })
                        }
                    }
                }
            }
            if (total != null && file.length() != total) throw IOException("받은 크기가 다릅니다")
        } finally {
            connection.disconnect()
        }
        return file
    }

    /** 세션에 APK를 쓰고 커밋한다. 결과는 [InstallResultReceiver]로 온다. 블로킹. */
    fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // 설치가 끝나면 앱이 바로 종료되므로 다음 실행 때 알림을 띄우도록 표시해 둔다.
            File(context.filesDir, UPDATED_MARKER).writeText(apk.nameWithoutExtension.substringAfter("YTLoader-"))
            // 시스템이 결과를 extra로 채워 넣으므로 mutable이어야 한다.
            val callback = PendingIntent.getBroadcast(
                context, sessionId, Intent(context, InstallResultReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
        apk.delete()
    }

    /** 지난번에 받다 만 APK 정리. */
    fun clearDownloads() {
        updateDir.deleteRecursively()
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
    }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/ytloader/android/releases/latest"
        private const val TIMEOUT_MS = 15_000
        /** 자기 업데이트 설치를 시작했다는 표시. 내용은 새 버전 태그. */
        const val UPDATED_MARKER = "app_update_pending"
    }
}
