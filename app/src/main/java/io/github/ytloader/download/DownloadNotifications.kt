package io.github.ytloader.download

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.ytloader.MainActivity
import io.github.ytloader.R

/** 알림 채널·빌더 (ARCHITECTURE 7장). */
class DownloadNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_PROGRESS, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.channel_progress)).build(),
                NotificationChannelCompat.Builder(CHANNEL_RESULT, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_result)).build(),
            ),
        )
    }

    /** [progress]: 0–100, null이면 진행률 미정. */
    fun progress(text: String, progress: Float?): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.notification_progress_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(1000, ((progress ?: 0f) * 10).toInt().coerceIn(0, 1000), progress == null)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.cancel), cancelIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun notifyProgress(text: String, progress: Float?) = post(ID_PROGRESS, progress(text, progress))

    /** 탭하면 첫 파일을 열고, [공유]·[폴더] 동작을 붙인다 (FR-7, FR-12). */
    fun showSucceeded(files: List<SavedFile>) {
        val first = files.firstOrNull()
        val tap = if (first == null) openAppIntent() else activity(REQ_VIEW, FileActions.openIntent(first))
        post(ID_RESULT, result(context.getString(R.string.notification_succeeded_title), context.getString(R.string.status_succeeded, savedLocation(files)), tap, files))
    }

    /** 실패해도 재생목록에서 저장한 파일이 있으면 [공유]·[폴더]를 붙인다. */
    fun showFailed(text: String, saved: List<SavedFile> = emptyList()) =
        post(ID_RESULT, result(context.getString(R.string.notification_failed_title), text, openAppIntent(), saved))

    /** 앱 자체 업데이트 후 첫 신호(MY_PACKAGE_REPLACED). 조용히 설치되면 앱이 닫히므로 다시 열 수 있게 한다. */
    fun showAppUpdated(version: String) = post(
        ID_APP_UPDATE,
        NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.app_update_done_title))
            .setContentText(context.getString(R.string.app_update_done_text, version))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build(),
    )

    private fun savedLocation(files: List<SavedFile>) =
        FileActions.locationLabel(files.firstOrNull()?.folder, ShareRules.commonSubDir(files.map { it.subDir }))

    private fun result(title: String, text: String, tap: PendingIntent, files: List<SavedFile>): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tap)
        if (files.isNotEmpty()) {
            val shareTitle = context.getString(R.string.share)
            builder.addAction(0, shareTitle, activity(REQ_SHARE, FileActions.shareIntent(files, shareTitle)))
            FileActions.folderIntent(context, files)?.let {
                builder.addAction(0, context.getString(R.string.folder), activity(REQ_FOLDER, it))
            }
        }
        return builder.build()
    }

    private fun activity(requestCode: Int, intent: Intent): PendingIntent = PendingIntent.getActivity(
        context, requestCode, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun post(id: Int, notification: Notification) {
        // 권한이 없으면 조용히 건너뛴다 (진행은 앱 화면에서 보임 — SPEC 7장).
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        manager.notify(id, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, REQ_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelIntent(): PendingIntent = PendingIntent.getService(
        context, REQ_CANCEL,
        Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_PROGRESS = "download_progress"
        const val CHANNEL_RESULT = "download_result"
        const val ID_PROGRESS = 1
        const val ID_RESULT = 2
        const val ID_APP_UPDATE = 3
        private const val REQ_OPEN = 0
        private const val REQ_CANCEL = 1
        private const val REQ_VIEW = 2
        private const val REQ_SHARE = 3
        private const val REQ_FOLDER = 4
    }
}
