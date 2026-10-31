package io.github.ytloader.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat
import io.github.ytloader.R
import io.github.ytloader.YTLoaderApp
import io.github.ytloader.download.DownloadNotifications
import java.io.File

/**
 * 앱 자체 업데이트 결과 (FR-15).
 * - PackageInstaller 세션 결과: 확인이 필요하면 시스템 설치 화면을 띄우고, 실패하면 화면 상태에 알린다.
 * - MY_PACKAGE_REPLACED: 새 버전이 설치됐다. 이 앱이 시작한 업데이트였으면 완료 알림을 띄운다.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val marker = File(context.filesDir, AppUpdater.UPDATED_MARKER)
            if (marker.isFile) {
                DownloadNotifications(context).showAppUpdated(marker.readText())
                marker.delete()
            }
            return
        }
        val container = (context.applicationContext as YTLoaderApp).container
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        Log.i("YTLoader", "app update install status $status")
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // 곧 프로세스가 새 버전으로 교체된다
            else -> {
                File(context.filesDir, AppUpdater.UPDATED_MARKER).delete()
                val current = container.appUpdate.value
                val release = when (current) {
                    is AppUpdateState.Installing -> current.release
                    is AppUpdateState.Downloading -> current.release
                    else -> return
                }
                // 사용자가 설치 화면에서 취소했으면 다시 누를 수 있게 되돌린다.
                container.appUpdate.value = if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
                    AppUpdateState.Available(release)
                } else {
                    val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: status.toString()
                    AppUpdateState.Failed(release, context.getString(R.string.app_update_install_failed, detail))
                }
            }
        }
    }
}
